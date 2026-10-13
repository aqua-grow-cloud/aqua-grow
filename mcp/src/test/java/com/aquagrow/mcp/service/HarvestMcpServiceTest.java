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
class HarvestMcpServiceTest {

    @Mock FarmDataClient farmData;
    @Mock ResolverService resolver;
    private HarvestMcpService service;

    @BeforeEach
    void setUp() {
        service = new HarvestMcpService(farmData, resolver);
        when(resolver.resolveAnyBatch("pond 3")).thenReturn(new ResolvedBatch("b-1", "Sept", List.of("Pond 3"), null, "Active"));
    }

    @Test
    void harvestLogs_allAndByBatch() {
        HarvestLog log = HarvestLog.builder().batchId("b-1").date(LocalDate.of(2026, 10, 5)).revenue(100.0).build();
        when(farmData.harvestLogs()).thenReturn(List.of(log));
        when(farmData.harvestLogsByBatch("b-1")).thenReturn(List.of(log));

        assertThat(service.listHarvestLogs()).containsExactly(log);
        assertThat(service.listHarvestLogsByBatch("pond 3")).containsExactly(log);
    }

    @Test
    void fishStockEvents_allAndByBatch() {
        FishStockEvent event = FishStockEvent.builder().batchId("b-1").type(FishStockEventType.SAMPLING).build();
        when(farmData.fishStockEvents()).thenReturn(List.of(event));
        when(farmData.fishStockEventsByBatch("b-1")).thenReturn(List.of(event));

        assertThat(service.listFishStockEvents()).containsExactly(event);
        assertThat(service.listFishStockEventsByBatch("pond 3")).containsExactly(event);
    }

    @Test
    void totalRevenue_isTheNetRevenueSummed_andSaysSo() {
        when(farmData.harvestLogsByBatch("b-1")).thenReturn(List.of(
                HarvestLog.builder().revenue(95000.0).commission(5000.0).build(),
                HarvestLog.builder().revenue(100000.0).commission(0.0).build(),
                HarvestLog.builder().revenue(null).build()));

        BatchTotal total = service.getTotalRevenueByBatch("pond 3");

        assertThat(total.batchId()).isEqualTo("b-1");
        assertThat(total.batchName()).isEqualTo("Sept");
        assertThat(total.total()).isEqualTo(195000.0);
        assertThat(total.records()).isEqualTo(3);
        assertThat(total.notes()).anyMatch(n -> n.contains("net of commission"))
                .anyMatch(n -> n.contains("1 harvest log(s) have no revenue"));
    }
}
