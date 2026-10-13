package com.aquagrow.mcp.config;

import com.aquagrow.mcp.security.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterActivatorTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void activate_withTenantInContext_enablesHibernateFilterAndReturnsTenantId() {
        TenantContext.setTenantId("root-farm");
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);

        TenantFilterActivator activator = new TenantFilterActivator();
        setEntityManager(activator, entityManager);

        String result = activator.activate();

        assertThat(result).isEqualTo("root-farm");
        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("tenantId", "root-farm");
    }

    @Test
    void activate_withNoTenantInContext_throwsIllegalStateException() {
        TenantFilterActivator activator = new TenantFilterActivator();
        setEntityManager(activator, entityManager);

        assertThatThrownBy(activator::activate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No tenant bound");
    }

    private void setEntityManager(TenantFilterActivator activator, EntityManager em) {
        try {
            var field = TenantFilterActivator.class.getDeclaredField("entityManager");
            field.setAccessible(true);
            field.set(activator, em);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
