package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.*;
import com.aquagrow.api.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HarvestService.
 * Tests business logic in isolation using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HarvestService Tests")
class HarvestServiceTest {

    @Mock
    private HarvestLogRepository harvestLogRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private PondRepository pondRepository;

    @Mock
    private FishStockEventRepository fishStockEventRepository;

    @Mock
    private FishTypeRepository fishTypeRepository;

    @InjectMocks
    private HarvestService harvestService;

    private static final String TEST_TENANT_ID = "tenant-123";
    private static final String TEST_BATCH_ID = "batch-456";
    private static final String TEST_POND_ID = "pond-789";
    private static final String TEST_FISH_TYPE_ID = "fishtype-101";
    private static final double TEST_BIOMASS = 500.0;
    private static final double TEST_BASIC_RATE = 100.0;

    /**
     * Stubs the fish-type lookup required by {@code validateAndRecalculateHarvest}.
     * Uses SHRIMP cultivation so the recalculated rate equals the basic rate (no discount).
     */
    private void stubShrimpFishType() {
        FishType fishType = FishType.builder()
                .id(TEST_FISH_TYPE_ID)
                .name("Vannamei")
                .cultivationType(CultivationType.SHRIMP)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(fishTypeRepository.findById(TEST_FISH_TYPE_ID)).thenReturn(Optional.of(fishType));
    }

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TEST_TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========== GET TESTS ==========

    @Test
    @DisplayName("getAllHarvestLogs - should return all logs when no filters")
    void getAllHarvestLogs_shouldReturnAllLogsWhenNoFilters() {
        // Given
        List<HarvestLog> logs = List.of(
                HarvestLog.builder().id("log-1").build(),
                HarvestLog.builder().id("log-2").build()
        );
        when(harvestLogRepository.findAll(any(Example.class))).thenReturn(logs);

        // When
        List<HarvestLog> result = harvestService.getAllHarvestLogs(null, null, null);

        // Then
        assertThat(result).hasSize(2);
        verify(harvestLogRepository).findAll(any(Example.class));
    }

    @Test
    @DisplayName("getAllHarvestLogs - should filter by batchId")
    void getAllHarvestLogs_shouldFilterByBatchId() {
        // Given
        List<HarvestLog> logs = List.of(
                HarvestLog.builder().id("log-1").batchId(TEST_BATCH_ID).build()
        );
        when(harvestLogRepository.findAll(any(Example.class))).thenReturn(logs);

        // When
        List<HarvestLog> result = harvestService.getAllHarvestLogs(TEST_BATCH_ID, null, null);

        // Then
        assertThat(result).hasSize(1);
        verify(harvestLogRepository).findAll(any(Example.class));
    }

    @Test
    @DisplayName("getHarvestLogsByBatch - should return logs for specific batch")
    void getHarvestLogsByBatch_shouldReturnLogsForSpecificBatch() {
        // Given
        List<HarvestLog> logs = List.of(
                HarvestLog.builder().batchId(TEST_BATCH_ID).build()
        );
        when(harvestLogRepository.findByBatchId(TEST_BATCH_ID)).thenReturn(logs);

        // When
        List<HarvestLog> result = harvestService.getHarvestLogsByBatch(TEST_BATCH_ID);

        // Then
        assertThat(result).hasSize(1);
        verify(harvestLogRepository).findByBatchId(TEST_BATCH_ID);
    }

    // ========== RECORD HARVEST TESTS ==========

    @Test
    @DisplayName("recordHarvest - should set tenant ID and save log")
    void recordHarvest_shouldSetTenantIdAndSaveLog() {
        // Given
        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(100)
                .avgWeightGrams(250.0)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .date(LocalDate.now())
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .batchId(TEST_BATCH_ID)
                .pondId(TEST_POND_ID)
                .count(100)
                .tenantId(TEST_TENANT_ID)
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);

        // When
        HarvestLog result = harvestService.recordHarvest(log);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(log.getTenantId()).isEqualTo(TEST_TENANT_ID);
        verify(harvestLogRepository).save(log);
    }

    @Test
    @DisplayName("recordHarvest - should throw exception when tenant context is null")
    void recordHarvest_shouldThrowExceptionWhenTenantContextIsNull() {
        // Given
        TenantContext.clear();
        HarvestLog log = HarvestLog.builder().build();

        // When / Then
        assertThatThrownBy(() -> harvestService.recordHarvest(log))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    @DisplayName("recordHarvest - should create harvest stock event")
    void recordHarvest_shouldCreateHarvestStockEvent() {
        // Given
        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(150)
                .avgWeightGrams(300.0)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .date(LocalDate.now())
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .batchId(TEST_BATCH_ID)
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(150)
                .avgWeightGrams(300.0)
                .tenantId(TEST_TENANT_ID)
                .date(LocalDate.now())
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);

        // When
        harvestService.recordHarvest(log);

        // Then
        ArgumentCaptor<FishStockEvent> eventCaptor = ArgumentCaptor.forClass(FishStockEvent.class);
        verify(fishStockEventRepository).save(eventCaptor.capture());

        FishStockEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getType()).isEqualTo(FishStockEventType.HARVEST);
        assertThat(capturedEvent.getQuantityDelta()).isEqualTo(-150);  // Negative for harvest
        assertThat(capturedEvent.getPondId()).isEqualTo(TEST_POND_ID);
        assertThat(capturedEvent.getBatchId()).isEqualTo(TEST_BATCH_ID);
        assertThat(capturedEvent.getTenantId()).isEqualTo(TEST_TENANT_ID);
    }

    @Test
    @DisplayName("recordHarvest - should not create stock event when count is null")
    void recordHarvest_shouldNotCreateStockEventWhenCountIsNull() {
        // Given
        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .count(null)  // No count
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .tenantId(TEST_TENANT_ID)
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);

        // When
        harvestService.recordHarvest(log);

        // Then
        verify(fishStockEventRepository, never()).save(any());
    }

    // ========== FINAL HARVEST TESTS ==========

    @Test
    @DisplayName("recordHarvest - should mark seeds as harvested for final harvest")
    void recordHarvest_shouldMarkSeedsAsHarvestedForFinalHarvest() {
        // Given
        FishSeed seed1 = FishSeed.builder()
                .id("seed-1")
                .fishTypeId(TEST_FISH_TYPE_ID)
                .pondId(TEST_POND_ID)
                .status("Active")
                .build();

        FishSeed seed2 = FishSeed.builder()
                .id("seed-2")
                .fishTypeId("other-type")
                .pondId(TEST_POND_ID)
                .status("Active")
                .build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed1, seed2)))
                .build();

        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(100)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .isFinal(true)  // Final harvest
                .date(LocalDate.now())
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .batchId(TEST_BATCH_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .tenantId(TEST_TENANT_ID)
                .count(100)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);
        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));

        // When
        harvestService.recordHarvest(log);

        // Then
        assertThat(seed1.getStatus()).isEqualTo("Harvested");
        assertThat(seed2.getStatus()).isEqualTo("Active");  // Different type, should remain active
        verify(batchRepository).save(batch);
    }

    @Test
    @DisplayName("recordHarvest - should reject harvest when fishTypeId is null")
    void recordHarvest_shouldRejectWhenFishTypeIdIsNull() {
        // Given: a harvest without a fish type. Harvesting "all" seeds via a null
        // fishTypeId is not a valid case and the feature has been removed; recordHarvest
        // must reject it and never touch the batch.
        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .fishTypeId(null)  // No specific type
                .count(100)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        // When / Then
        assertThatThrownBy(() -> harvestService.recordHarvest(log))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fish type is required");

        verify(harvestLogRepository, never()).save(any());
        verify(batchRepository, never()).findById(any());
    }

    @Test
    @DisplayName("recordHarvest - should clear pond assignment when no active seeds remain")
    void recordHarvest_shouldClearPondAssignmentWhenNoActiveSeedsRemain() {
        // Given
        FishSeed seed = FishSeed.builder()
                .id("seed-1")
                .fishTypeId(TEST_FISH_TYPE_ID)
                .pondId(TEST_POND_ID)
                .status("Active")
                .build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        Pond pond = Pond.builder()
                .id(TEST_POND_ID)
                .currentBatchId(TEST_BATCH_ID)
                .build();

        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(100)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .batchId(TEST_BATCH_ID)
                .tenantId(TEST_TENANT_ID)
                .count(100)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);
        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));
        when(pondRepository.findById(TEST_POND_ID)).thenReturn(Optional.of(pond));

        // When
        harvestService.recordHarvest(log);

        // Then
        assertThat(pond.getCurrentBatchId()).isNull();
        verify(pondRepository).save(pond);
    }

    @Test
    @DisplayName("recordHarvest - should not clear pond assignment when active seeds remain")
    void recordHarvest_shouldNotClearPondAssignmentWhenActiveSeedsRemain() {
        // Given
        FishSeed seed1 = FishSeed.builder()
                .id("seed-1")
                .fishTypeId(TEST_FISH_TYPE_ID)
                .pondId(TEST_POND_ID)
                .status("Active")
                .build();

        FishSeed seed2 = FishSeed.builder()
                .id("seed-2")
                .fishTypeId("other-type")
                .pondId(TEST_POND_ID)
                .status("Active")
                .build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed1, seed2)))
                .build();

        Pond pond = Pond.builder()
                .id(TEST_POND_ID)
                .currentBatchId(TEST_BATCH_ID)
                .build();

        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)  // Only harvest this type
                .count(100)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .batchId(TEST_BATCH_ID)
                .tenantId(TEST_TENANT_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(100)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);
        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));

        // When
        harvestService.recordHarvest(log);

        // Then
        assertThat(pond.getCurrentBatchId()).isEqualTo(TEST_BATCH_ID);  // Still assigned
        verify(pondRepository, never()).save(pond);  // Should not save pond
    }

    @Test
    @DisplayName("recordHarvest - should not process final harvest when isFinal is false")
    void recordHarvest_shouldNotProcessFinalHarvestWhenIsFinalIsFalse() {
        // Given
        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(100)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .isFinal(false)  // Not final
                .date(LocalDate.now())
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .tenantId(TEST_TENANT_ID)
                .count(100)
                .date(LocalDate.now())
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);

        // When
        harvestService.recordHarvest(log);

        // Then
        verify(batchRepository, never()).findById(any());
    }

    @Test
    @DisplayName("recordHarvest - should handle multiple ponds in final harvest")
    void recordHarvest_shouldHandleMultiplepondsInFinalHarvest() {
        // Given
        FishSeed seed1 = FishSeed.builder()
                .id("seed-1")
                .fishTypeId(TEST_FISH_TYPE_ID)
                .pondId("pond-A")
                .status("Active")
                .build();

        FishSeed seed2 = FishSeed.builder()
                .id("seed-2")
                .fishTypeId(TEST_FISH_TYPE_ID)
                .pondId("pond-B")
                .status("Active")
                .build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed1, seed2)))
                .build();

        Pond pondA = Pond.builder().id("pond-A").currentBatchId(TEST_BATCH_ID).build();
        Pond pondB = Pond.builder().id("pond-B").currentBatchId(TEST_BATCH_ID).build();

        HarvestLog log = HarvestLog.builder()
                .batchId(TEST_BATCH_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .count(200)
                .biomassWeight(TEST_BIOMASS)
                .basicRate(TEST_BASIC_RATE)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        HarvestLog savedLog = HarvestLog.builder()
                .id("harvest-1")
                .batchId(TEST_BATCH_ID)
                .tenantId(TEST_TENANT_ID)
                .count(200)
                .isFinal(true)
                .date(LocalDate.now())
                .build();

        stubShrimpFishType();
        when(harvestLogRepository.save(any(HarvestLog.class))).thenReturn(savedLog);
        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));
        when(pondRepository.findById("pond-A")).thenReturn(Optional.of(pondA));
        when(pondRepository.findById("pond-B")).thenReturn(Optional.of(pondB));

        // When
        harvestService.recordHarvest(log);

        // Then
        assertThat(pondA.getCurrentBatchId()).isNull();
        assertThat(pondB.getCurrentBatchId()).isNull();
        verify(pondRepository, times(2)).save(any(Pond.class));
    }
}
