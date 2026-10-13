package com.aquagrow.mcp.config;

import com.aquagrow.mcp.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Utility to activate the Hibernate tenantFilter on the current JPA session.
 * Each MCP tool calls this before querying/persisting to enforce tenant isolation.
 *
 * The tenant is always taken from {@link TenantContext} (populated by
 * TenantFilter from the authenticated JWT) — never from an LLM-supplied
 * tool parameter — so a Claude conversation cannot access another
 * tenant's data no matter what it is asked or prompted to do.
 */
@Component
public class TenantFilterActivator {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Activates the tenant filter using the tenant bound to the current
     * request by {@link TenantContext}.
     *
     * @throws IllegalStateException if no tenant is bound to the current thread
     */
    public String activate() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("No tenant bound to the current request context");
        }
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        return tenantId;
    }
}
