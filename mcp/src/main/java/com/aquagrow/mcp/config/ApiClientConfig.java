package com.aquagrow.mcp.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ApiClientConfig {

    @Bean
    @Qualifier("aquaGrowApiRestClient")
    public RestClient aquaGrowApiRestClient(
            @Value("${aquagrow.mcp.api.base-url}") String baseUrl,
            @Value("${aquagrow.mcp.api.connect-timeout:PT3S}") Duration connectTimeout,
            @Value("${aquagrow.mcp.api.read-timeout:PT15S}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
