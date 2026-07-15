package com.aquagrow.api.controller;

import com.aquagrow.api.dto.ChatRequest;
import com.aquagrow.api.dto.ChatResponse;
import com.aquagrow.api.dto.McpChatRequest;
import com.aquagrow.api.dto.McpChatResponse;
import com.aquagrow.api.service.McpChatService;
import com.aquagrow.api.service.OllamaChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AiChatController {

    private final OllamaChatService ollamaChatService;
    private final McpChatService mcpChatService;

    /** Legacy direct Ollama chat (no MCP tools) */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return ollamaChatService.chat(request);
    }

    /**
     * MCP-powered chat: the AI can query live farm data via MCP tools.
     * The tenantId is automatically extracted from the JWT in the request context.
     */
    @PostMapping("/mcp-chat")
    public McpChatResponse mcpChat(@RequestBody McpChatRequest request) {
        return mcpChatService.chat(request);
    }

    /**
     * Clear conversation history for a session.
     */
    @DeleteMapping("/mcp-chat/{conversationId}")
    public void clearConversation(@PathVariable String conversationId) {
        mcpChatService.clearConversation(conversationId);
    }

    @GetMapping("/models")
    public Map<String, Object> listModels() {
        return ollamaChatService.listModels();
    }
}
