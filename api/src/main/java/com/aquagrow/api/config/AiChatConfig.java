package com.aquagrow.api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring AI ChatClient with:
 * - Ollama as the underlying LLM (auto-configured via spring.ai.ollama properties)
 * - MCP tools from aqua-grow-mcp (auto-configured via spring.ai.mcp.client properties)
 * - An in-memory chat memory store for per-session conversation history
 */
@Configuration
public class AiChatConfig {

    private static final String SYSTEM_PROMPT = """
        You are an AI assistant for the Aqua Grow aquaculture management platform.
        You help farm managers and operators by answering questions about their ponds,
        batches, fish growth, water quality, feed consumption, harvests, and finances.
        
        You have access to tools that can query live farm data from the database.
        Always use the appropriate tools to fetch real data before answering questions.
        
        When a user asks about their farm data, extract the tenantId from the context
        and pass it to every tool call you make. The tenantId is provided in this prompt:
        TENANT_ID: {tenantId}
        
        Be concise, data-driven, and highlight any anomalies or actionable insights.
        Format numbers clearly (e.g. "5.2 kg", "MYR 1,200", "87% survival rate").
        """;

    @Bean
    public ChatClient mcpChatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem(SYSTEM_PROMPT)
            .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }
}
