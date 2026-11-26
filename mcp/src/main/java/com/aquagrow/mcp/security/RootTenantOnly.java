package com.aquagrow.mcp.security;

import com.aquagrow.mcp.client.ToolUserException;

/**
 * Employee and salary data is limited to the main (root) farm account. The API itself returns it to any
 * signed-in user of a farm, so this check in the MCP is the only guard on this path.
 */
public final class RootTenantOnly {

    public static final String MESSAGE = "Employee and salary data is only available to the main farm account.";

    private RootTenantOnly() {
    }

    public static void require() {
        if (TenantContext.isSubTenant()) {
            throw new ToolUserException(MESSAGE);
        }
    }
}
