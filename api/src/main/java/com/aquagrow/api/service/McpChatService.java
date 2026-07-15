package com.aquagrow.api.service;

import com.aquagrow.api.dto.McpChatRequest;
import com.aquagrow.api.dto.McpChatResponse;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI chat service powered by Spring AI ChatClient + MCP tools from aqua-grow-mcp.
 *
 * The ChatClient is pre-configured with:
 * - Ollama as the underlying LLM
 * - MCP tool callbacks (auto-discovered from aqua-grow-mcp via SSE)
 * - In-memory conversation memory per session
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpChatService {

    private final ChatClient mcpChatClient;
    private final ChatMemory chatMemory;

    @Value("${spring.ai.ollama.chat.options.model:llama3}")
    private String defaultModel;

    public McpChatResponse chat(McpChatRequest request) {
        String tenantId = TenantContext.getTenantId();
        String conversationId = request.getConversationId();

        log.info("MCP chat request: tenant={}, session={}, message={}",
            tenantId, conversationId, request.getMessage());

        String response = mcpChatClient.prompt()
            .system(sp -> sp.param("tenantId", tenantId != null ? tenantId : "unknown"))
            .user(request.getMessage())
            .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build())
            .call()
            .content();

        log.info("MCP chat response: tenant={}, session={}, responseLength={}",
            tenantId, conversationId, response != null ? response.length() : 0);

        return McpChatResponse.builder()
            .message(response)
            .conversationId(conversationId)
            .model(defaultModel)
            .toolsUsed(true)
            .build();
    }

    /**
     * Clear the conversation memory for a given session.
     */
    public void clearConversation(String conversationId) {
        chatMemory.clear(conversationId);
        log.info("Cleared conversation memory for session: {}", conversationId);
    }
}
