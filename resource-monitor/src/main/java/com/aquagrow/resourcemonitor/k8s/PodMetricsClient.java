package com.aquagrow.resourcemonitor.k8s;

import com.aquagrow.resourcemonitor.model.PodSnapshot;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PodMetricsClient {

    private final KubernetesClient client;

    public PodMetricsClient(KubernetesClient client) {
        this.client = client;
    }

    public List<PodSnapshot> fetchSnapshots(String namespace, List<String> selectors) {
        List<PodSnapshot> snapshots = new ArrayList<>();
        for (List<PodSnapshot> selectorSnapshots : fetchSnapshotsBySelector(namespace, selectors).values()) {
            snapshots.addAll(selectorSnapshots);
        }
        return snapshots;
    }

    /**
     * Same as {@link #fetchSnapshots(String, List)} but keeps the per-selector grouping so
     * callers can detect which individual selectors matched zero pods, rather than only being
     * able to tell that the aggregate result across ALL selectors was empty.
     */
    public Map<String, List<PodSnapshot>> fetchSnapshotsBySelector(String namespace, List<String> selectors) {
        Map<String, List<PodSnapshot>> snapshotsBySelector = new LinkedHashMap<>();
        List<Event> events = fetchEvents(namespace);
        Map<String, PodMetrics> metricsByPodName = fetchUsageForNamespace(namespace);
        for (String selector : selectors) {
            List<Pod> pods = client.pods().inNamespace(namespace).withLabelSelector(selector).list().getItems();
            List<PodSnapshot> snapshots = new ArrayList<>();
            for (Pod pod : pods) {
                snapshots.addAll(toSnapshots(pod, events, metricsByPodName));
            }
            snapshotsBySelector.put(selector, snapshots);
        }
        return snapshotsBySelector;
    }

    private List<PodSnapshot> toSnapshots(Pod pod, List<Event> events, Map<String, PodMetrics> metricsByPodName) {
        List<PodSnapshot> result = new ArrayList<>();
        String podName = pod.getMetadata().getName();
        String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
        List<String> eventReasons = filterEventReasons(events, podName);

        Map<String, ContainerStatus> statusByName = new HashMap<>();
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                statusByName.put(cs.getName(), cs);
            }
        }

        Map<String, Long> usageByContainer = usageFromMetrics(metricsByPodName.get(podName));

        if (pod.getSpec() == null) {
            return result;
        }

        for (var container : pod.getSpec().getContainers()) {
            String containerName = container.getName();
            ContainerStatus status = statusByName.get(containerName);
            boolean ready = status != null && Boolean.TRUE.equals(status.getReady());
            int restartCount = status != null && status.getRestartCount() != null ? status.getRestartCount() : 0;

            Long cpuLimit = quantityToMillicores(container.getResources(), true);
            Long memLimit = quantityToBytes(container.getResources(), true);

            Long cpuUsage = usageByContainer.get(containerName + ":cpu");
            Long memUsage = usageByContainer.get(containerName + ":memory");

            result.add(new PodSnapshot(
                    podName, containerName,
                    cpuUsage, memUsage,
                    cpuLimit, memLimit,
                    restartCount, ready, phase,
                    eventReasons));
        }
        return result;
    }

    /**
     * Fetches ALL events in the namespace ONCE per {@link #fetchSnapshots(String, List)} invocation.
     * Never throws; returns an empty list if the events API call fails.
     */
    private List<Event> fetchEvents(String namespace) {
        try {
            return client.v1().events().inNamespace(namespace).list().getItems();
        } catch (Exception e) {
            log.warn("Failed to fetch events for namespace {}: {}", namespace, e.getMessage());
            return List.of();
        }
    }

    private List<String> filterEventReasons(List<Event> events, String podName) {
        List<String> reasons = new ArrayList<>();
        for (Event event : events) {
            if (event.getInvolvedObject() != null
                    && podName.equals(event.getInvolvedObject().getName())
                    && event.getReason() != null) {
                reasons.add(event.getReason());
            }
        }
        return reasons;
    }

    /**
     * Best-effort read from the metrics.k8s.io aggregation API for ALL pods in the namespace,
     * fetched ONCE per {@link #fetchSnapshots(String, List)} invocation. Returns an empty map
     * (never throws) if metrics-server is unavailable or hasn't scraped anything yet.
     */
    private Map<String, PodMetrics> fetchUsageForNamespace(String namespace) {
        try {
            PodMetricsList podMetricsList = client.top().pods().metrics(namespace);
            Map<String, PodMetrics> metricsByPodName = new HashMap<>();
            if (podMetricsList != null && podMetricsList.getItems() != null) {
                for (PodMetrics podMetrics : podMetricsList.getItems()) {
                    if (podMetrics.getMetadata() != null) {
                        metricsByPodName.put(podMetrics.getMetadata().getName(), podMetrics);
                    }
                }
            }
            return metricsByPodName;
        } catch (Exception e) {
            log.debug("No metrics available for namespace {} (metrics-server may not have scraped yet): {}",
                    namespace, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Long> usageFromMetrics(PodMetrics podMetrics) {
        Map<String, Long> usage = new HashMap<>();
        if (podMetrics != null && podMetrics.getContainers() != null) {
            podMetrics.getContainers().forEach(cm -> {
                Quantity cpu = cm.getUsage().get("cpu");
                Quantity mem = cm.getUsage().get("memory");
                if (cpu != null) {
                    usage.put(cm.getName() + ":cpu", quantityToMillicoresValue(cpu));
                }
                if (mem != null) {
                    usage.put(cm.getName() + ":memory", quantityToBytesValue(mem));
                }
            });
        }
        return usage;
    }

    private Long quantityToMillicores(ResourceRequirements resources, boolean limit) {
        if (resources == null) return null;
        Map<String, Quantity> map = limit ? resources.getLimits() : resources.getRequests();
        if (map == null || !map.containsKey("cpu")) return null;
        return quantityToMillicoresValue(map.get("cpu"));
    }

    private Long quantityToBytes(ResourceRequirements resources, boolean limit) {
        if (resources == null) return null;
        Map<String, Quantity> map = limit ? resources.getLimits() : resources.getRequests();
        if (map == null || !map.containsKey("memory")) return null;
        return quantityToBytesValue(map.get("memory"));
    }

    /**
     * fabric8's {@link Quantity#getAmountInBytes} normalizes any suffixed quantity (SI or binary)
     * into its base numeric value: "500m" -> 0.5, "1" -> 1, "2" -> 2. For CPU quantities that base
     * unit is "cores", so multiplying by 1000 yields millicores.
     */
    private Long quantityToMillicoresValue(Quantity quantity) {
        BigDecimal amount = Quantity.getAmountInBytes(quantity);
        return amount == null ? null : amount.multiply(BigDecimal.valueOf(1000)).longValue();
    }

    /**
     * For memory quantities, the base unit of {@link Quantity#getAmountInBytes} is already bytes
     * (e.g. "512Mi" -> 536870912), so no further conversion is needed.
     */
    private Long quantityToBytesValue(Quantity quantity) {
        BigDecimal amount = Quantity.getAmountInBytes(quantity);
        return amount == null ? null : amount.longValue();
    }
}
