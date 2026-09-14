package com.aquagrow.resourcemonitor.config;

import com.aquagrow.resourcemonitor.k8s.PodMetricsClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesClientConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    public PodMetricsClient podMetricsClient(KubernetesClient client) {
        return new PodMetricsClient(client);
    }
}
