package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedLogRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private FeedLogRepository feedLogRepository;

    @Mock
    private FeedStockRepository feedStockRepository;

    @InjectMocks
    private FeedService feedService;

    private static final String TENANT_ID = "tenant-123";
    private static final String BATCH_ID = "batch-1";
    private static final String FEED_TYPE = "Premium Feed";

    private FeedLog testFeedLog;
    private FeedStock testFeedStock;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        testFeedLog = FeedLog.builder()
                .id("log-1")
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .feedType(FEED_TYPE)
                .amount(50.0)
                .time("08:00")
                .tenantId(TENANT_ID)
                .build();

        testFeedStock = FeedStock.builder()
                .id("stock-1")
                .feedType(FEED_TYPE)
                .currentStock(500.0)
                .threshold(100.0)
                .unit("kg")
                .tenantId(TENANT_ID)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createFeedLog_shouldSetTenantId() {
        // Given
        FeedLog newLog = FeedLog.builder()
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .feedType(FEED_TYPE)
                .amount(50.0)
                .build();

        when(feedStockRepository.findByTenantIdAndFeedType(TENANT_ID, FEED_TYPE))
                .thenReturn(Optional.of(testFeedStock));
        when(feedLogRepository.save(any(FeedLog.class))).thenReturn(testFeedLog);

        // When
        FeedLog result = feedService.createFeedLog(newLog);

        // Then
        ArgumentCaptor<FeedLog> captor = ArgumentCaptor.forClass(FeedLog.class);
        verify(feedLogRepository).save(captor.capture());

        FeedLog savedLog = captor.getValue();
        assertThat(savedLog.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result).isEqualTo(testFeedLog);
    }

    @Test
    void createFeedLog_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();
        FeedLog newLog = FeedLog.builder()
                .batchId(BATCH_ID)
                .amount(50.0)
                .build();

        // When & Then
        assertThatThrownBy(() -> feedService.createFeedLog(newLog))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant context is required");

        verify(feedLogRepository, never()).save(any());
    }

    @Test
    void createFeedLog_shouldDeductFromInventory() {
        // Given
        FeedLog newLog = FeedLog.builder()
                .batchId(BATCH_ID)
                .feedType(FEED_TYPE)
                .amount(50.0)
                .build();

        when(feedStockRepository.findByTenantIdAndFeedType(TENANT_ID, FEED_TYPE))
                .thenReturn(Optional.of(testFeedStock));
        when(feedLogRepository.save(any(FeedLog.class))).thenReturn(testFeedLog);

        // When
        feedService.createFeedLog(newLog);

        // Then
        ArgumentCaptor<FeedStock> stockCaptor = ArgumentCaptor.forClass(FeedStock.class);
        verify(feedStockRepository).save(stockCaptor.capture());

        FeedStock updatedStock = stockCaptor.getValue();
        assertThat(updatedStock.getCurrentStock()).isEqualTo(450.0); // 500 - 50
    }

    @Test
    void createFeedLog_shouldThrowException_whenInsufficientStock() {
        // Given
        FeedLog newLog = FeedLog.builder()
                .batchId(BATCH_ID)
                .feedType(FEED_TYPE)
                .amount(600.0) // More than available 500
                .build();

        when(feedStockRepository.findByTenantIdAndFeedType(TENANT_ID, FEED_TYPE))
                .thenReturn(Optional.of(testFeedStock));

        // When & Then
        assertThatThrownBy(() -> feedService.createFeedLog(newLog))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient feed stock");

        verify(feedLogRepository, never()).save(any());
    }

    @Test
    void createFeedLog_shouldNotDeductInventory_whenFeedTypeIsNull() {
        // Given
        FeedLog newLog = FeedLog.builder()
                .batchId(BATCH_ID)
                .feedType(null)
                .amount(50.0)
                .build();

        when(feedLogRepository.save(any(FeedLog.class))).thenReturn(testFeedLog);

        // When
        feedService.createFeedLog(newLog);

        // Then
        verify(feedStockRepository, never()).findByTenantIdAndFeedType(any(), any());
        verify(feedStockRepository, never()).save(any());
        verify(feedLogRepository).save(any(FeedLog.class));
    }

    @Test
    void createFeedLog_shouldNotDeductInventory_whenFeedStockNotFound() {
        // Given
        FeedLog newLog = FeedLog.builder()
                .batchId(BATCH_ID)
                .feedType("Unknown Feed")
                .amount(50.0)
                .build();

        when(feedStockRepository.findByTenantIdAndFeedType(TENANT_ID, "Unknown Feed"))
                .thenReturn(Optional.empty());
        when(feedLogRepository.save(any(FeedLog.class))).thenReturn(testFeedLog);

        // When
        feedService.createFeedLog(newLog);

        // Then
        verify(feedStockRepository, never()).save(any());
        verify(feedLogRepository).save(any(FeedLog.class));
    }

    @Test
    void getAllFeedLogs_shouldReturnAllLogs() {
        // Given
        FeedLog log2 = FeedLog.builder().id("log-2").build();
        List<FeedLog> logs = Arrays.asList(testFeedLog, log2);
        when(feedLogRepository.findAll()).thenReturn(logs);

        // When
        List<FeedLog> result = feedService.getAllFeedLogs();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testFeedLog, log2);
        verify(feedLogRepository).findAll();
    }

    @Test
    void getFeedLogsByBatch_shouldReturnLogsForBatch() {
        // Given
        FeedLog log2 = FeedLog.builder().id("log-2").batchId(BATCH_ID).build();
        List<FeedLog> logs = Arrays.asList(testFeedLog, log2);
        when(feedLogRepository.findByBatchId(BATCH_ID)).thenReturn(logs);

        // When
        List<FeedLog> result = feedService.getFeedLogsByBatch(BATCH_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().allMatch(log -> log.getBatchId().equals(BATCH_ID))).isTrue();
        verify(feedLogRepository).findByBatchId(BATCH_ID);
    }

    @Test
    void calculateTotalFeedConsumed_shouldSumAllAmounts() {
        // Given
        FeedLog log1 = FeedLog.builder().amount(50.0).build();
        FeedLog log2 = FeedLog.builder().amount(75.0).build();
        FeedLog log3 = FeedLog.builder().amount(25.0).build();
        when(feedLogRepository.findByBatchId(BATCH_ID))
                .thenReturn(Arrays.asList(log1, log2, log3));

        // When
        Double result = feedService.calculateTotalFeedConsumed(BATCH_ID);

        // Then
        assertThat(result).isEqualTo(150.0); // 50 + 75 + 25
    }

    @Test
    void calculateTotalFeedConsumed_shouldReturnZero_whenNoLogs() {
        // Given
        when(feedLogRepository.findByBatchId(BATCH_ID)).thenReturn(Collections.emptyList());

        // When
        Double result = feedService.calculateTotalFeedConsumed(BATCH_ID);

        // Then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void calculateTotalFeedConsumed_shouldHandleNullAmounts() {
        // Given
        FeedLog log1 = FeedLog.builder().amount(50.0).build();
        FeedLog log2 = FeedLog.builder().amount(null).build();
        FeedLog log3 = FeedLog.builder().amount(25.0).build();
        when(feedLogRepository.findByBatchId(BATCH_ID))
                .thenReturn(Arrays.asList(log1, log2, log3));

        // When
        Double result = feedService.calculateTotalFeedConsumed(BATCH_ID);

        // Then
        assertThat(result).isEqualTo(75.0); // 50 + 0 + 25
    }

    @Test
    void calculateFeedConsumedInDateRange_shouldFilterByDateRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().minusDays(2);

        FeedLog log1 = FeedLog.builder().date(LocalDate.now().minusDays(5)).amount(50.0).build();
        FeedLog log2 = FeedLog.builder().date(LocalDate.now().minusDays(15)).amount(30.0).build(); // Outside range
        FeedLog log3 = FeedLog.builder().date(LocalDate.now().minusDays(3)).amount(20.0).build();
        when(feedLogRepository.findByBatchId(BATCH_ID))
                .thenReturn(Arrays.asList(log1, log2, log3));

        // When
        Double result = feedService.calculateFeedConsumedInDateRange(BATCH_ID, startDate, endDate);

        // Then
        assertThat(result).isEqualTo(70.0); // 50 + 20 (log2 is outside range)
    }

    @Test
    void calculateFeedBreakdownByType_shouldGroupByFeedType() {
        // Given
        FeedLog log1 = FeedLog.builder().feedType("Feed A").amount(50.0).build();
        FeedLog log2 = FeedLog.builder().feedType("Feed B").amount(30.0).build();
        FeedLog log3 = FeedLog.builder().feedType("Feed A").amount(20.0).build();
        when(feedLogRepository.findByBatchId(BATCH_ID))
                .thenReturn(Arrays.asList(log1, log2, log3));

        // When
        Map<String, Double> result = feedService.calculateFeedBreakdownByType(BATCH_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("Feed A")).isEqualTo(70.0); // 50 + 20
        assertThat(result.get("Feed B")).isEqualTo(30.0);
    }

    @Test
    void calculateFeedBreakdownByType_shouldIgnoreNullFeedTypes() {
        // Given
        FeedLog log1 = FeedLog.builder().feedType("Feed A").amount(50.0).build();
        FeedLog log2 = FeedLog.builder().feedType(null).amount(30.0).build();
        when(feedLogRepository.findByBatchId(BATCH_ID))
                .thenReturn(Arrays.asList(log1, log2));

        // When
        Map<String, Double> result = feedService.calculateFeedBreakdownByType(BATCH_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("Feed A")).isEqualTo(50.0);
    }

    @Test
    void calculateAverageDailyFeed_shouldCalculateCorrectAverage() {
        // Given
        LocalDate date1 = LocalDate.now().minusDays(5);
        LocalDate date2 = LocalDate.now().minusDays(4);
        LocalDate date3 = LocalDate.now().minusDays(3);

        FeedLog log1 = FeedLog.builder().date(date1).amount(60.0).build();
        FeedLog log2 = FeedLog.builder().date(date1).amount(40.0).build(); // Same day as log1
        FeedLog log3 = FeedLog.builder().date(date2).amount(50.0).build();
        FeedLog log4 = FeedLog.builder().date(date3).amount(70.0).build();
        when(feedLogRepository.findByBatchId(BATCH_ID))
                .thenReturn(Arrays.asList(log1, log2, log3, log4));

        // When
        Double result = feedService.calculateAverageDailyFeed(BATCH_ID);

        // Then
        // Day 1: 100kg (60+40), Day 2: 50kg, Day 3: 70kg
        // Average: 220 / 3 days = 73.33
        assertThat(result).isCloseTo(73.33, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void calculateAverageDailyFeed_shouldReturnZero_whenNoLogs() {
        // Given
        when(feedLogRepository.findByBatchId(BATCH_ID)).thenReturn(Collections.emptyList());

        // When
        Double result = feedService.calculateAverageDailyFeed(BATCH_ID);

        // Then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void calculateFCR_shouldCalculateCorrectRatio() {
        // Given
        Double totalFeed = 150.0; // kg
        Double initialBiomass = 50.0; // kg
        Double currentBiomass = 100.0; // kg
        // Biomass gain: 100 - 50 = 50 kg
        // FCR: 150 / 50 = 3.0

        // When
        Double result = feedService.calculateFCR(BATCH_ID, totalFeed, initialBiomass, currentBiomass);

        // Then
        assertThat(result).isEqualTo(3.0);
    }

    @Test
    void calculateFCR_shouldReturnNull_whenBiomassGainIsZero() {
        // Given
        Double totalFeed = 150.0;
        Double initialBiomass = 100.0;
        Double currentBiomass = 100.0; // No gain

        // When
        Double result = feedService.calculateFCR(BATCH_ID, totalFeed, initialBiomass, currentBiomass);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void calculateFCR_shouldReturnNull_whenBiomassGainIsNegative() {
        // Given
        Double totalFeed = 150.0;
        Double initialBiomass = 100.0;
        Double currentBiomass = 80.0; // Loss

        // When
        Double result = feedService.calculateFCR(BATCH_ID, totalFeed, initialBiomass, currentBiomass);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void calculateFCR_shouldReturnNull_whenParametersAreNull() {
        // When & Then
        assertThat(feedService.calculateFCR(BATCH_ID, null, 50.0, 100.0)).isNull();
        assertThat(feedService.calculateFCR(BATCH_ID, 150.0, null, 100.0)).isNull();
        assertThat(feedService.calculateFCR(BATCH_ID, 150.0, 50.0, null)).isNull();
    }

    @Test
    void getLowStockAlerts_shouldReturnLowStockItems() {
        // Given
        FeedStock lowStock1 = FeedStock.builder()
                .feedType("Feed A")
                .currentStock(50.0)
                .threshold(100.0)
                .build();
        FeedStock lowStock2 = FeedStock.builder()
                .feedType("Feed B")
                .currentStock(20.0)
                .threshold(100.0)
                .build();

        when(feedStockRepository.findLowStockByTenantId(TENANT_ID))
                .thenReturn(Arrays.asList(lowStock1, lowStock2));

        // When
        List<FeedStock> result = feedService.getLowStockAlerts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(lowStock1, lowStock2);
        verify(feedStockRepository).findLowStockByTenantId(TENANT_ID);
    }

    @Test
    void getLowStockAlerts_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();

        // When & Then
        assertThatThrownBy(() -> feedService.getLowStockAlerts())
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant context is required");
    }
}
