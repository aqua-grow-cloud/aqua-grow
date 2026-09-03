package com.aquagrow.api.service;

import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedLogRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the global-feed-inventory bug: a sub-tenant logging feed must deduct from
 * the ROOT tenant's global stock, even though the globally-enabled Hibernate tenantFilter
 * (TenantAspect) is pinned to the current sub-tenant id. The filter otherwise blocks both
 * the deductStock UPDATE and the findByTenantIdAndFeedType lookup, producing a spurious
 * "not found in global inventory" error.
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FeedServiceHierarchyTest {

    @Autowired
    private FeedLogRepository feedLogRepository;

    @Autowired
    private FeedStockRepository feedStockRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void createFeedLog_subTenant_deductsFromRootGlobalStock_whenFilterPinnedToSubTenant() {
        // Global feed stock lives at the ROOT tenant
        FeedStock stock = new FeedStock();
        stock.setId("stock-1");
        stock.setTenantId("root-farm");
        stock.setFeedType("King Fish Feed");
        stock.setCurrentStock(1000.0);
        stock.setThreshold(50.0);
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

        FeedService feedService = new FeedService(feedLogRepository, feedStockRepository, entityManager);

        FeedLog feedLog = new FeedLog();
        feedLog.setBatchId("batch-1");
        feedLog.setFeedType("King Fish Feed");
        feedLog.setAmount(10.0);
        feedLog.setDate(LocalDate.now());

        FeedLog saved = feedService.createFeedLog(feedLog);

        // FeedLog belongs to the sub-tenant
        assertThat(saved.getTenantId()).isEqualTo("sub-farm");

        // Root global stock must have been deducted 1000 -> 990
        entityManager.flush();
        entityManager.clear();
        FeedStock rootStock = feedStockRepository.findById("stock-1").orElseThrow();
        assertThat(rootStock.getCurrentStock()).isEqualTo(990.0);
    }
}
