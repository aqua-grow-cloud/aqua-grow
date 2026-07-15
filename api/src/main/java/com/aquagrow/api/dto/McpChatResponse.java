package com.aquagrow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from the MCP-powered AI chat endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpChatResponse {
    private String message;
    private String conversationId;
    private String model;
    private boolean toolsUsed;
}
