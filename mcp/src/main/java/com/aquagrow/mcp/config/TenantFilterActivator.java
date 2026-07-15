package com.aquagrow.mcp.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Utility to activate the Hibernate tenantFilter on the current JPA session.
 * Each MCP tool calls this before querying/persisting to enforce tenant isolation.
 */
@Component
public class TenantFilterActivator {

    @PersistenceContext
    private EntityManager entityManager;

    public void activateFor(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be null or blank");
        }
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
}
