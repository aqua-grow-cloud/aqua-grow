package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.model.*;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedMcpServiceTest {

    @Mock FarmDataClient farmData;
    @Mock ResolverService resolver;
    private FeedMcpService service;

    @BeforeEach
    void setUp() {
        service = new FeedMcpService(farmData, resolver);
    }

    private void givenBatch() {
        when(resolver.resolveAnyBatch("pond 3")).thenReturn(new ResolvedBatch("b-1", "Sept", List.of("Pond 3"), null, "Active"));
    }

    @Test
    void feedLogs_allAndByBatch() {
        FeedLog log = FeedLog.builder().batchId("b-1").date(LocalDate.of(2026, 10, 1)).amount(10.0).build();
        when(farmData.feedLogs()).thenReturn(List.of(log));
        givenBatch();
        when(farmData.feedLogsByBatch("b-1")).thenReturn(List.of(log));

        assertThat(service.listFeedLogs()).containsExactly(log);
        assertThat(service.listFeedLogsByBatch("pond 3")).containsExactly(log);
    }

    @Test
    void feedStock_andLowStockAlerts() {
        FeedStock ok = FeedStock.builder().feedType("King").currentStock(500.0).threshold(100.0).build();
        FeedStock low = FeedStock.builder().feedType("Starter").currentStock(5.0).threshold(100.0).build();
        when(farmData.feedStock()).thenReturn(List.of(ok, low));
        when(farmData.lowStock()).thenReturn(List.of(low));

        assertThat(service.listFeedStock()).containsExactly(ok, low);
        assertThat(service.getLowStockAlerts()).containsExactly(low);
    }

    @Test
    void feedOrders_allAndOnlyPending() {
        FeedOrder pending = FeedOrder.builder().id("o-1").feedType("King").quantity(500.0).status(OrderStatus.PENDING).supplierId("s").orderDate(LocalDateTime.now()).build();
        FeedOrder received = FeedOrder.builder().id("o-2").feedType("King").quantity(100.0).status(OrderStatus.RECEIVED).supplierId("s").orderDate(LocalDateTime.now()).build();
        when(farmData.feedOrders()).thenReturn(List.of(pending, received));

        assertThat(service.listFeedOrders()).containsExactly(pending, received);
        assertThat(service.listPendingFeedOrders()).containsExactly(pending);
    }

    @Test
    void anAmbiguousBatchNameIsReportedNotGuessed() {
        when(resolver.resolveAnyBatch("tilapia")).thenThrow(new ResolverService.AmbiguousReferenceException("More than one batch"));

        assertThatThrownBy(() -> service.listFeedLogsByBatch("tilapia")).isInstanceOf(ResolverService.AmbiguousReferenceException.class);
        verify(farmData, never()).feedLogsByBatch("tilapia");
    }
}
