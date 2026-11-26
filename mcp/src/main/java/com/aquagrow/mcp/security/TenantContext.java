package com.aquagrow.mcp.security;

/**
 * Holds the tenant identity for the current MCP tool invocation thread.
 * Populated by {@link TenantFilter} from the authenticated JWT's tenant_id claim.
 * Mirrors com.aquagrow.api.security.TenantContext in aqua-grow-api.
 */
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> parentTenantId = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void setParentTenantId(String id) {
        parentTenantId.set(id);
    }

    public static String getParentTenantId() {
        return parentTenantId.get();
    }

    public static boolean isSubTenant() {
        return parentTenantId.get() != null;
    }

    public static boolean isRootTenant() {
        return parentTenantId.get() == null;
    }

    public static void clear() {
        currentTenant.remove();
        parentTenantId.remove();
    }
}
