package com.aquagrow.mcp.mcp;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Hides state-changing tools from MCP clients unless explicitly enabled.
 *
 * The legacy {@code create_*}, {@code update_*} and {@code delete_*} tools
 * write straight to the database and bypass aqua-grow-api business rules
 * (stock deduction, stock events, validation). They stay hidden until each is
 * moved behind a preview/confirm flow. The {@code confirm_*} tools are not
 * affected by this filter.
 */
public class WriteToolFilteringProvider implements ToolCallbackProvider {

    private static final List<String> WRITE_PREFIXES = List.of("create_", "update_", "delete_");

    private final ToolCallbackProvider delegate;
    private final boolean legacyWriteToolsEnabled;

    public WriteToolFilteringProvider(ToolCallbackProvider delegate, boolean legacyWriteToolsEnabled) {
        this.delegate = delegate;
        this.legacyWriteToolsEnabled = legacyWriteToolsEnabled;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        ToolCallback[] all = delegate.getToolCallbacks();
        if (legacyWriteToolsEnabled) {
            return all;
        }
        return Arrays.stream(all)
                .filter(cb -> !isLegacyWriteTool(cb.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
    }

    static boolean isLegacyWriteTool(String name) {
        return WRITE_PREFIXES.stream().anyMatch(name::startsWith);
    }
}
