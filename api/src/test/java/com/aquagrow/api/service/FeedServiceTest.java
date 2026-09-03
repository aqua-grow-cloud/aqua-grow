package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedLogRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private FeedLogRepository feedLogRepository;
    @Mock private FeedStockRepository feedStockRepository;
    @Mock private EntityManager entityManager;
    @InjectMocks private FeedService feedService;

    @BeforeEach
    void setUpTenantFilterStubs() {
        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
        lenient().when(session.enableFilter("tenantFilter")).thenReturn(filter);
        lenient().when(filter.setParameter(anyString(), any())).thenReturn(filter);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void createFeedLog_forSubTenant_deductsFromRootStock() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");

        FeedLog feedLog = new FeedLog();
        feedLog.setBatchId("batch-1");
        feedLog.setFeedType("Pellet A");
        feedLog.setAmount(10.0);

        when(feedStockRepository.deductStock(eq("root-farm"), eq("Pellet A"), eq(10.0), any()))
                .thenReturn(1);
        when(feedLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedLog saved = feedService.createFeedLog(feedLog);

        // FeedLog tenantId should be the sub-tenant, not the root
        assertThat(saved.getTenantId()).isEqualTo("branch-1");
        // Deduction should target root-farm
        verify(feedStockRepository).deductStock(eq("root-farm"), eq("Pellet A"), eq(10.0), any());
    }

    @Test
    void createFeedLog_forRootTenant_deductsFromOwnStock() {
        TenantContext.setTenantId("root-farm");

        FeedLog feedLog = new FeedLog();
        feedLog.setBatchId("batch-1");
        feedLog.setFeedType("Pellet A");
        feedLog.setAmount(10.0);

        when(feedStockRepository.deductStock(eq("root-farm"), eq("Pellet A"), eq(10.0), any()))
                .thenReturn(1);
        when(feedLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        feedService.createFeedLog(feedLog);

        verify(feedStockRepository).deductStock(eq("root-farm"), eq("Pellet A"), eq(10.0), any());
    }

    @Test
    void createFeedLog_insufficientStock_throwsBusinessException() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");

        FeedLog feedLog = new FeedLog();
        feedLog.setFeedType("Pellet A");
        feedLog.setAmount(50.0);

        when(feedStockRepository.deductStock(anyString(), anyString(), anyDouble(), any()))
                .thenReturn(0);
        FeedStock existingStock = new FeedStock();
        existingStock.setCurrentStock(10.0);
        when(feedStockRepository.findByTenantIdAndFeedType("root-farm", "Pellet A"))
                .thenReturn(Optional.of(existingStock));

        assertThatThrownBy(() -> feedService.createFeedLog(feedLog))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient global feed stock");
    }

    @Test
    void createFeedLog_feedTypeNotInGlobalInventory_throwsBusinessException() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");

        FeedLog feedLog = new FeedLog();
        feedLog.setFeedType("Unknown Feed");
        feedLog.setAmount(10.0);

        when(feedStockRepository.deductStock(anyString(), anyString(), anyDouble(), any()))
                .thenReturn(0);
        when(feedStockRepository.findByTenantIdAndFeedType("root-farm", "Unknown Feed"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.createFeedLog(feedLog))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found in global inventory");
    }

    @Test
    void getLowStockAlerts_forSubTenant_queriesRootTenantStock() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");

        when(feedStockRepository.findLowStockByTenantId("root-farm")).thenReturn(java.util.List.of());

        feedService.getLowStockAlerts();

        verify(feedStockRepository).findLowStockByTenantId("root-farm");
    }
}
