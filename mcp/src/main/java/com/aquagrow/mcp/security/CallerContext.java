package com.aquagrow.mcp.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Snapshot of who is calling: the tenant (and parent tenant) from the validated JWT plus the
 * Spring Security context that holds the token.
 *
 * The MCP server runs synchronous tool handlers on a separate scheduler thread, so the
 * thread-locals set while the HTTP request was authenticated are not visible there. Reactor's
 * automatic context propagation uses this class to copy them onto the worker thread for the
 * duration of the call and to clear them afterwards. See {@link ContextPropagationConfig}.
 */
public final class CallerContext {

    private CallerContext() {
    }

    public record Snapshot(String tenantId, String parentTenantId, SecurityContext securityContext) {
    }

    /** @return the caller bound to this thread, or null if there is none. */
    static Snapshot capture() {
        String tenant = TenantContext.getTenantId();
        SecurityContext security = SecurityContextHolder.getContext();
        boolean hasSecurity = security != null && security.getAuthentication() != null;
        if (tenant == null && !hasSecurity) {
            return null;
        }
        return new Snapshot(tenant, TenantContext.getParentTenantId(), hasSecurity ? security : null);
    }

    static void restore(Snapshot snapshot) {
        TenantContext.clear();
        if (snapshot.tenantId() != null) {
            TenantContext.setTenantId(snapshot.tenantId());
        }
        if (snapshot.parentTenantId() != null) {
            TenantContext.setParentTenantId(snapshot.parentTenantId());
        }
        if (snapshot.securityContext() != null) {
            SecurityContextHolder.setContext(snapshot.securityContext());
        } else {
            SecurityContextHolder.clearContext();
        }
    }

    static void reset() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }
}
