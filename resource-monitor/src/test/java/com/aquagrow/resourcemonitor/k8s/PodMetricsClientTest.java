package com.aquagrow.resourcemonitor.k8s;

import com.aquagrow.resourcemonitor.model.PodSnapshot;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKubernetesMockClient(crud = true)
class PodMetricsClientTest {

    KubernetesClient client;

    @Test
    void fetchSnapshotsReturnsPodStatusAndEventsEvenWithoutMetricsServer() {
        client.pods().inNamespace("aqua-grow").resource(
                new PodBuilder()
                        .withNewMetadata().withName("aqua-grow-api-abc").withNamespace("aqua-grow")
                        .addToLabels("app", "aqua-grow-api").endMetadata()
                        .withNewSpec()
                        .addNewContainer().withName("api")
                        .withNewResources()
                        .addToRequests("cpu", new io.fabric8.kubernetes.api.model.Quantity("100m"))
                        .addToRequests("memory", new io.fabric8.kubernetes.api.model.Quantity("256Mi"))
                        .addToLimits("cpu", new io.fabric8.kubernetes.api.model.Quantity("500m"))
                        .addToLimits("memory", new io.fabric8.kubernetes.api.model.Quantity("512Mi"))
                        .endResources()
                        .endContainer()
                        .endSpec()
                        .withNewStatus()
                        .withPhase("Running")
                        .addNewContainerStatus()
                        .withName("api").withReady(true).withRestartCount(0)
                        .endContainerStatus()
                        .endStatus()
                        .build()
        ).create();

        client.v1().events().inNamespace("aqua-grow").resource(
                new EventBuilder()
                        .withNewMetadata().withName("evt-1").withNamespace("aqua-grow").endMetadata()
                        .withReason("BackOff")
                        .withNewInvolvedObject().withName("aqua-grow-api-abc").withKind("Pod").endInvolvedObject()
                        .build()
        ).create();

        PodMetricsClient metricsClient = new PodMetricsClient(client);
        List<PodSnapshot> snapshots = metricsClient.fetchSnapshots("aqua-grow", List.of("app=aqua-grow-api"));

        assertThat(snapshots).hasSize(1);
        PodSnapshot snapshot = snapshots.get(0);
        assertThat(snapshot.podName()).isEqualTo("aqua-grow-api-abc");
        assertThat(snapshot.containerName()).isEqualTo("api");
        assertThat(snapshot.ready()).isTrue();
        assertThat(snapshot.phase()).isEqualTo("Running");
        assertThat(snapshot.restartCount()).isZero();
        assertThat(snapshot.cpuLimitMillicores()).isEqualTo(500L);
        assertThat(snapshot.memLimitBytes()).isEqualTo(512L * 1024 * 1024);
        assertThat(snapshot.recentEventReasons()).contains("BackOff");
        // No metrics-server registered in this mock cluster, so usage fields are null — must not throw.
        assertThat(snapshot.cpuUsageMillicores()).isNull();
        assertThat(snapshot.memUsageBytes()).isNull();
    }
}
