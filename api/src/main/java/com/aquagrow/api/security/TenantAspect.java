package com.aquagrow.api.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantAspect {

    @PersistenceContext
    private EntityManager entityManager;

    // Pointcut for service methods (assumed transactional boundary)
    // Adjust package name 'com.aquagrow.api.service' as needed for your project
    // structure
    // Or target repositories if services are thin.
    // Ideally, this should run whenever a Session is opened.
    @Before("execution(* com.aquagrow.api.controller..*(..))")
    public void enableTenantFilter() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        }
    }
}
