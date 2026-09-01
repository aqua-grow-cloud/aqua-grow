package com.aquagrow.api.security;

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

    /**
     * Returns the tenant_id that owns the global feed stock.
     * For sub-tenants, this is the parent (root) tenant.
     * For root tenants, this is their own tenant_id.
     */
    public static String getEffectiveFeedTenantId() {
        return isSubTenant() ? parentTenantId.get() : currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
        parentTenantId.remove();
    }
}
