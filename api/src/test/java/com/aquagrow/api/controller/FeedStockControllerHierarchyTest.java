package com.aquagrow.api.controller;

import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantAuthorizationService;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the tenant-hierarchy feed-stock bug: a sub-tenant must be able to read
 * its root/parent tenant's feed stock, even though the globally-enabled Hibernate
 * tenantFilter (see TenantAspect) is pinned to the current sub-tenant id.
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FeedStockControllerHierarchyTest {

    @Autowired
    private FeedStockRepository feedStockRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void getAllFeedStock_subTenantReadsRootStock_whenTenantFilterPinnedToSubTenant() {
        // Root-level stock exists
        FeedStock stock = new FeedStock();
        stock.setId("s1");
        stock.setTenantId("root-farm");
        stock.setFeedType("Pellet A");
        stock.setCurrentStock(100.0);
        stock.setThreshold(20.0);
        stock.setUnit("kg");
        feedStockRepository.save(stock);
        entityManager.flush();
        entityManager.clear();

        // Simulate TenantAspect: tenantFilter enabled and pinned to the current SUB-tenant
        entityManager.unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", "sub-farm");

        // Sub-tenant request context (parent/root = root-farm)
        TenantContext.setTenantId("sub-farm");
        TenantContext.setParentTenantId("root-farm");

        FeedStockController controller = new FeedStockController(
                feedStockRepository, new TenantAuthorizationService(), entityManager);

        List<FeedStock> result = controller.getAllFeedStock();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo("root-farm");
    }
}
