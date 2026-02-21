package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.MonitoringLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.MonitoringLogRepository;
import com.aquagrow.api.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private MonitoringLogRepository monitoringLogRepository;

    @Mock
    private BatchRepository batchRepository;

    @InjectMocks
    private MonitoringService monitoringService;

    private static final String TENANT_ID = "tenant-123";
    private static final String BATCH_ID = "batch-1";
    private static final String POND_ID = "pond-1";

    private MonitoringLog testLog;
    private Batch testBatch;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        testLog = MonitoringLog.builder()
                .id("log-1")
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .doLevel(7.5)
                .ph(8.0)
                .temperature(28.0)
                .ammonia(0.1)
                .salinity(15.0)
                .turbidity(20.0)
                .tenantId(TENANT_ID)
                .build();

        testBatch = Batch.builder()
                .id(BATCH_ID)
                .tenantId(TENANT_ID)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createMonitoringLog_shouldSetTenantId() {
        // Given
        MonitoringLog newLog = MonitoringLog.builder()
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .doLevel(7.5)
                .ph(8.0)
                .build();

        when(monitoringLogRepository.save(any(MonitoringLog.class))).thenReturn(testLog);

        // When
        MonitoringLog result = monitoringService.createMonitoringLog(newLog);

        // Then
        ArgumentCaptor<MonitoringLog> captor = ArgumentCaptor.forClass(MonitoringLog.class);
        verify(monitoringLogRepository).save(captor.capture());

        MonitoringLog savedLog = captor.getValue();
        assertThat(savedLog.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result).isEqualTo(testLog);
    }

    @Test
    void createMonitoringLog_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();
        MonitoringLog newLog = MonitoringLog.builder()
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .build();

        // When & Then
        assertThatThrownBy(() -> monitoringService.createMonitoringLog(newLog))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant context is required");

        verify(monitoringLogRepository, never()).save(any());
    }

    @Test
    void createMonitoringLog_shouldSaveWithAllWaterQualityParameters() {
        // Given
        MonitoringLog fullLog = MonitoringLog.builder()
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .doLevel(7.5)
                .ph(8.0)
                .temperature(28.0)
                .ammonia(0.1)
                .salinity(15.0)
                .turbidity(20.0)
                .build();

        when(monitoringLogRepository.save(any(MonitoringLog.class))).thenReturn(testLog);

        // When
        MonitoringLog result = monitoringService.createMonitoringLog(fullLog);

        // Then
        verify(monitoringLogRepository).save(any(MonitoringLog.class));
        assertThat(result.getDoLevel()).isEqualTo(7.5);
        assertThat(result.getPh()).isEqualTo(8.0);
        assertThat(result.getTemperature()).isEqualTo(28.0);
        assertThat(result.getAmmonia()).isEqualTo(0.1);
        assertThat(result.getSalinity()).isEqualTo(15.0);
        assertThat(result.getTurbidity()).isEqualTo(20.0);
    }

    @Test
    void searchMonitoringLogs_shouldFilterByBatchId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MonitoringLog> expectedPage = new PageImpl<>(Collections.singletonList(testLog));

        when(monitoringLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MonitoringLog> result = monitoringService.searchMonitoringLogs(BATCH_ID, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testLog);
        verify(monitoringLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchMonitoringLogs_shouldFilterByPondId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MonitoringLog> expectedPage = new PageImpl<>(Collections.singletonList(testLog));

        when(monitoringLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MonitoringLog> result = monitoringService.searchMonitoringLogs(null, POND_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(monitoringLogRepository).findAll(any(Specification.class), eq(pageable));
        // Note: batchRepository is called inside the Specification lambda, which only executes
        // when the actual query runs. Since we mock the repository, we can't verify this call.
    }

    @Test
    void searchMonitoringLogs_shouldReturnEmpty_whenNoBatchesForPond() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MonitoringLog> emptyPage = new PageImpl<>(Collections.emptyList());

        when(monitoringLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(emptyPage);

        // When
        Page<MonitoringLog> result = monitoringService.searchMonitoringLogs(null, POND_ID, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(monitoringLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchMonitoringLogs_shouldFilterByBothBatchAndPond() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MonitoringLog> expectedPage = new PageImpl<>(Collections.singletonList(testLog));

        when(monitoringLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MonitoringLog> result = monitoringService.searchMonitoringLogs(BATCH_ID, POND_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(monitoringLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchMonitoringLogs_shouldReturnAllLogs_whenNoFiltersProvided() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        MonitoringLog log2 = MonitoringLog.builder()
                .id("log-2")
                .batchId("batch-2")
                .date(LocalDate.now())
                .build();
        Page<MonitoringLog> expectedPage = new PageImpl<>(Arrays.asList(testLog, log2));

        when(monitoringLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MonitoringLog> result = monitoringService.searchMonitoringLogs(null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(monitoringLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchMonitoringLogs_shouldThrowException_whenPageableIsNull() {
        // When & Then
        assertThatThrownBy(() -> monitoringService.searchMonitoringLogs(null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Pageable cannot be null");
    }

    @Test
    void searchMonitoringLogs_shouldHandleEmptyBatchId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MonitoringLog> expectedPage = new PageImpl<>(Collections.singletonList(testLog));

        when(monitoringLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MonitoringLog> result = monitoringService.searchMonitoringLogs("", null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(monitoringLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchMonitoringLogs_shouldHandleEmptyPondId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MonitoringLog> expectedPage = new PageImpl<>(Collections.singletonList(testLog));

        when(monitoringLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MonitoringLog> result = monitoringService.searchMonitoringLogs(null, "", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(monitoringLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMonitoringLogsByBatch_shouldReturnAllLogsForBatch() {
        // Given
        MonitoringLog log2 = MonitoringLog.builder()
                .id("log-2")
                .batchId(BATCH_ID)
                .date(LocalDate.now().minusDays(1))
                .build();
        List<MonitoringLog> expectedLogs = Arrays.asList(testLog, log2);

        when(monitoringLogRepository.findByBatchId(BATCH_ID)).thenReturn(expectedLogs);

        // When
        List<MonitoringLog> result = monitoringService.getMonitoringLogsByBatch(BATCH_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testLog, log2);
        verify(monitoringLogRepository).findByBatchId(BATCH_ID);
    }

    @Test
    void getMonitoringLogsByBatch_shouldReturnEmpty_whenNoBatchFound() {
        // Given
        when(monitoringLogRepository.findByBatchId("non-existent-batch"))
                .thenReturn(Collections.emptyList());

        // When
        List<MonitoringLog> result = monitoringService.getMonitoringLogsByBatch("non-existent-batch");

        // Then
        assertThat(result).isEmpty();
        verify(monitoringLogRepository).findByBatchId("non-existent-batch");
    }

    @Test
    void getMonitoringLogsByBatch_shouldReturnLogsInChronologicalOrder() {
        // Given
        MonitoringLog log1 = MonitoringLog.builder()
                .id("log-1")
                .batchId(BATCH_ID)
                .date(LocalDate.now().minusDays(2))
                .build();
        MonitoringLog log2 = MonitoringLog.builder()
                .id("log-2")
                .batchId(BATCH_ID)
                .date(LocalDate.now().minusDays(1))
                .build();
        MonitoringLog log3 = MonitoringLog.builder()
                .id("log-3")
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .build();
        List<MonitoringLog> expectedLogs = Arrays.asList(log1, log2, log3);

        when(monitoringLogRepository.findByBatchId(BATCH_ID)).thenReturn(expectedLogs);

        // When
        List<MonitoringLog> result = monitoringService.getMonitoringLogsByBatch(BATCH_ID);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDate()).isBefore(result.get(1).getDate());
        assertThat(result.get(1).getDate()).isBefore(result.get(2).getDate());
    }
}
