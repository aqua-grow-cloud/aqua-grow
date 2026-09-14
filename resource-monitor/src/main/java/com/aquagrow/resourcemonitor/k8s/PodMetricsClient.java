package com.aquagrow.resourcemonitor.k8s;

import com.aquagrow.resourcemonitor.model.PodSnapshot;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
        for (String selector : selectors) {
            List<Pod> pods = client.pods().inNamespace(namespace).withLabelSelector(selector).list().getItems();
            for (Pod pod : pods) {
                snapshots.addAll(toSnapshots(pod, namespace));
            }
        }
        return snapshots;
    }

    private List<PodSnapshot> toSnapshots(Pod pod, String namespace) {
        List<PodSnapshot> result = new ArrayList<>();
        String podName = pod.getMetadata().getName();
        String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
        List<String> eventReasons = fetchRecentEventReasons(namespace, podName);

        Map<String, ContainerStatus> statusByName = new HashMap<>();
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                statusByName.put(cs.getName(), cs);
            }
        }

        Map<String, Long> usageByContainer = fetchUsage(namespace, podName);

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

    private List<String> fetchRecentEventReasons(String namespace, String podName) {
        try {
            List<Event> events = client.v1().events().inNamespace(namespace).list().getItems();
            List<String> reasons = new ArrayList<>();
            for (Event event : events) {
                if (event.getInvolvedObject() != null
                        && podName.equals(event.getInvolvedObject().getName())
                        && event.getReason() != null) {
                    reasons.add(event.getReason());
                }
            }
            return reasons;
        } catch (Exception e) {
            log.warn("Failed to fetch events for pod {}: {}", podName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Best-effort read from the metrics.k8s.io aggregation API. Returns an empty map
     * (never throws) if metrics-server is unavailable or the pod hasn't been scraped yet.
     */
    private Map<String, Long> fetchUsage(String namespace, String podName) {
        try {
            PodMetrics podMetrics = client.top().pods().metrics(namespace, podName);
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
        } catch (Exception e) {
            log.debug("No metrics available for pod {} (metrics-server may not have scraped yet): {}",
                    podName, e.getMessage());
            return Map.of();
        }
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
