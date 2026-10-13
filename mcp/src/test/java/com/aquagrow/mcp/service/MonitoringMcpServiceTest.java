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
class MonitoringMcpServiceTest {

    @Mock FarmDataClient farmData;
    @Mock ResolverService resolver;
    private MonitoringMcpService service;

    @BeforeEach
    void setUp() {
        service = new MonitoringMcpService(farmData, resolver);
        when(resolver.resolveAnyBatch("pond 3")).thenReturn(new ResolvedBatch("b-1", "Sept", List.of("Pond 3"), null, "Active"));
    }

    @Test
    void monitoringLogs_allAndByBatch() {
        MonitoringLog log = MonitoringLog.builder().batchId("b-1").date(LocalDate.of(2026, 10, 1)).doLevel(5.5).build();
        when(farmData.monitoringLogs()).thenReturn(List.of(log));
        when(farmData.monitoringLogsByBatch("b-1")).thenReturn(List.of(log));

        assertThat(service.listMonitoringLogs()).containsExactly(log);
        assertThat(service.listMonitoringLogsByBatch("pond 3")).containsExactly(log);
    }

    @Test
    void sampleLogs_allAndByBatch() {
        SampleLog log = SampleLog.builder().batchId("b-1").date(LocalDate.of(2026, 10, 1)).averageWeight(50.0).build();
        when(farmData.sampleLogs()).thenReturn(List.of(log));
        when(farmData.sampleLogsByBatch("b-1")).thenReturn(List.of(log));

        assertThat(service.listSampleLogs()).containsExactly(log);
        assertThat(service.listSampleLogsByBatch("pond 3")).containsExactly(log);
    }
}
