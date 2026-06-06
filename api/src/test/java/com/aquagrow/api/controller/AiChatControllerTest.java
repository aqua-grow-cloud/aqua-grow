package com.aquagrow.api.controller;

import com.aquagrow.api.dto.ChatMessage;
import com.aquagrow.api.dto.ChatRequest;
import com.aquagrow.api.dto.ChatResponse;
import com.aquagrow.api.service.OllamaChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiChatController.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/aquagrow",
    "cors.allowed-origins=http://localhost:3000"
})
@DisplayName("AiChatController Tests")
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OllamaChatService ollamaChatService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/ai/chat - Should return chat response")
    void chat_shouldReturnResponse() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.builder().role("user").content("Hello").build()
                ))
                .model("llama3")
                .build();

        ChatResponse response = ChatResponse.builder()
                .model("llama3")
                .message(ChatMessage.builder().role("assistant").content("Hi there!").build())
                .done(true)
                .build();

        when(ollamaChatService.chat(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("llama3"))
                .andExpect(jsonPath("$.message.content").value("Hi there!"))
                .andExpect(jsonPath("$.done").value(true));

        verify(ollamaChatService).chat(any(ChatRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ai/models - Should return list of models")
    void listModels_shouldReturnModels() throws Exception {
        Map<String, Object> models = Map.of("models", List.of(
                Map.of("name", "llama3:latest")
        ));

        when(ollamaChatService.listModels()).thenReturn(models);

        mockMvc.perform(get("/api/ai/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.models[0].name").value("llama3:latest"));

        verify(ollamaChatService).listModels();
    }

    @Test
    @DisplayName("POST /api/ai/chat - Should require authentication")
    void chat_shouldRequireAuthentication() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.builder().role("user").content("Hello").build()
                ))
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(ollamaChatService, never()).chat(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/ai/chat - Should return 400 when messages are empty")
    void chat_shouldReturnBadRequest_whenMessagesEmpty() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of())
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(ollamaChatService, never()).chat(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/ai/chat - Should return 400 when messages is null")
    void chat_shouldReturnBadRequest_whenMessagesNull() throws Exception {
        ChatRequest request = ChatRequest.builder().build();

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(ollamaChatService, never()).chat(any());
    }
}
