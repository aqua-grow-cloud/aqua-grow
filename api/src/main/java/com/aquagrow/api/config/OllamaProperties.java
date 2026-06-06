package com.aquagrow.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private String baseUrl = "http://localhost:11434";
    private String model = "llama3";
    private int connectTimeout = 30_000;
    private int readTimeout = 120_000;
}
