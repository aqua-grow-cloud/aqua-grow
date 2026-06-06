package com.aquagrow.api.service;

import com.aquagrow.api.config.OllamaProperties;
import com.aquagrow.api.dto.ChatRequest;
import com.aquagrow.api.dto.ChatResponse;
import com.aquagrow.api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaChatService {

    private final RestTemplate restTemplate;
    private final OllamaProperties ollamaProperties;

    public ChatResponse chat(ChatRequest request) {
        String model = request.getModel() != null ? request.getModel() : ollamaProperties.getModel();
        String url = ollamaProperties.getBaseUrl() + "/api/chat";

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", request.getMessages(),
                "stream", request.getStream() != null && request.getStream()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.debug("Sending chat request to Ollama: model={}, messages={}", model, request.getMessages().size());

        try {
            ResponseEntity<ChatResponse> response = restTemplate.postForEntity(url, entity, ChatResponse.class);
            log.debug("Received chat response from Ollama: model={}", response.getBody() != null ? response.getBody().getModel() : "null");
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to communicate with Ollama at {}: {}", url, e.getMessage());
            throw new BusinessException("AI service unavailable: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> listModels() {
        String url = ollamaProperties.getBaseUrl() + "/api/tags";

        log.debug("Fetching available models from Ollama");

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch models from Ollama at {}: {}", url, e.getMessage());
            throw new BusinessException("AI service unavailable: " + e.getMessage(), e);
        }
    }
}
