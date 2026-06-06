package com.aquagrow.api.service;

import com.aquagrow.api.config.OllamaProperties;
import com.aquagrow.api.dto.ChatMessage;
import com.aquagrow.api.dto.ChatRequest;
import com.aquagrow.api.dto.ChatResponse;
import com.aquagrow.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OllamaChatServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OllamaProperties ollamaProperties;

    @InjectMocks
    private OllamaChatService ollamaChatService;

    private ChatRequest chatRequest;
    private ChatResponse chatResponse;

    @BeforeEach
    void setUp() {
        when(ollamaProperties.getBaseUrl()).thenReturn("http://localhost:11434");
        when(ollamaProperties.getModel()).thenReturn("llama3");

        chatRequest = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.builder().role("user").content("Hello").build()
                ))
                .build();

        chatResponse = ChatResponse.builder()
                .model("llama3")
                .message(ChatMessage.builder().role("assistant").content("Hi there!").build())
                .done(true)
                .build();
    }

    @Test
    void chat_shouldUseDefaultModel_whenNotSpecified() {
        when(restTemplate.postForEntity(
                eq("http://localhost:11434/api/chat"),
                any(HttpEntity.class),
                eq(ChatResponse.class)
        )).thenReturn(ResponseEntity.ok(chatResponse));

        ChatResponse result = ollamaChatService.chat(chatRequest);

        assertThat(result).isNotNull();
        assertThat(result.getModel()).isEqualTo("llama3");
        assertThat(result.getMessage().getContent()).isEqualTo("Hi there!");

        verify(restTemplate).postForEntity(
                eq("http://localhost:11434/api/chat"),
                any(HttpEntity.class),
                eq(ChatResponse.class)
        );
    }

    @Test
    void chat_shouldUseCustomModel_whenSpecified() {
        chatRequest.setModel("mistral");

        ChatResponse mistralResponse = ChatResponse.builder()
                .model("mistral")
                .message(ChatMessage.builder().role("assistant").content("Hello from Mistral!").build())
                .done(true)
                .build();

        when(restTemplate.postForEntity(
                eq("http://localhost:11434/api/chat"),
                any(HttpEntity.class),
                eq(ChatResponse.class)
        )).thenReturn(ResponseEntity.ok(mistralResponse));

        ChatResponse result = ollamaChatService.chat(chatRequest);

        assertThat(result).isNotNull();
        assertThat(result.getModel()).isEqualTo("mistral");
        assertThat(result.getMessage().getContent()).isEqualTo("Hello from Mistral!");
    }

    @Test
    void chat_shouldThrowException_whenOllamaUnavailable() {
        when(restTemplate.postForEntity(
                eq("http://localhost:11434/api/chat"),
                any(HttpEntity.class),
                eq(ChatResponse.class)
        )).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> ollamaChatService.chat(chatRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI service unavailable");
    }

    @Test
    void listModels_shouldReturnModels() {
        Map<String, Object> expectedModels = Map.of("models", List.of(
                Map.of("name", "llama3:latest"),
                Map.of("name", "mistral:latest")
        ));

        when(restTemplate.getForEntity(
                eq("http://localhost:11434/api/tags"),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(expectedModels));

        Map<String, Object> result = ollamaChatService.listModels();

        assertThat(result).isNotNull();
        assertThat(result).containsKey("models");

        verify(restTemplate).getForEntity("http://localhost:11434/api/tags", Map.class);
    }

    @Test
    void listModels_shouldThrowException_whenOllamaUnavailable() {
        when(restTemplate.getForEntity(
                eq("http://localhost:11434/api/tags"),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> ollamaChatService.listModels())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI service unavailable");
    }
}
