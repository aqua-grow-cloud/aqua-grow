package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FishStockEvent;
import com.aquagrow.api.model.FishStockEventType;
import com.aquagrow.api.repository.FishStockEventRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FishStockEventServiceTest {

    @Mock
    private FishStockEventRepository fishStockEventRepository;

    @InjectMocks
    private FishStockEventService fishStockEventService;

    private static final String TENANT_ID = "tenant-123";
    private static final String BATCH_ID = "batch-1";
    private static final String POND_ID = "pond-1";
    private static final String FISH_TYPE_ID = "fish-type-1";

    private FishStockEvent seedEvent;
    private FishStockEvent harvestEvent;
    private FishStockEvent mortalityEvent;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        seedEvent = FishStockEvent.builder()
                .id("event-1")
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .date(LocalDate.now().minusDays(30))
                .type(FishStockEventType.SEED)
                .fishTypeId(FISH_TYPE_ID)
                .quantityDelta(10000)
                .avgWeightGram(5.0)
                .referenceId("seed-1")
                .tenantId(TENANT_ID)
                .build();

        harvestEvent = FishStockEvent.builder()
                .id("event-2")
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .date(LocalDate.now().minusDays(5))
                .type(FishStockEventType.HARVEST)
                .fishTypeId(FISH_TYPE_ID)
                .quantityDelta(-2000)
                .avgWeightGram(150.0)
                .referenceId("harvest-1")
                .tenantId(TENANT_ID)
                .build();

        mortalityEvent = FishStockEvent.builder()
                .id("event-3")
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .date(LocalDate.now().minusDays(10))
                .type(FishStockEventType.MORTALITY)
                .fishTypeId(FISH_TYPE_ID)
                .quantityDelta(-500)
                .avgWeightGram(120.0)
                .referenceId("mortality-1")
                .tenantId(TENANT_ID)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createStockEvent_shouldSetTenantId() {
        // Given
        FishStockEvent newEvent = FishStockEvent.builder()
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .type(FishStockEventType.SEED)
                .quantityDelta(1000)
                .build();

        when(fishStockEventRepository.save(any(FishStockEvent.class))).thenReturn(seedEvent);

        // When
        FishStockEvent result = fishStockEventService.createStockEvent(newEvent);

        // Then
        ArgumentCaptor<FishStockEvent> captor = ArgumentCaptor.forClass(FishStockEvent.class);
        verify(fishStockEventRepository).save(captor.capture());

        FishStockEvent savedEvent = captor.getValue();
        assertThat(savedEvent.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result).isEqualTo(seedEvent);
    }

    @Test
    void createStockEvent_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();
        FishStockEvent newEvent = FishStockEvent.builder()
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .type(FishStockEventType.SEED)
                .quantityDelta(1000)
                .build();

        // When & Then
        assertThatThrownBy(() -> fishStockEventService.createStockEvent(newEvent))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant context is required");

        verify(fishStockEventRepository, never()).save(any());
    }

    @Test
    void createStockEvent_shouldThrowException_whenDuplicateReferenceId() {
        // Given
        FishStockEvent newEvent = FishStockEvent.builder()
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .type(FishStockEventType.HARVEST)
                .quantityDelta(-100)
                .referenceId("harvest-1")
                .build();

        when(fishStockEventRepository.existsByTypeAndReferenceId(
                FishStockEventType.HARVEST, "harvest-1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> fishStockEventService.createStockEvent(newEvent))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock event already exists");

        verify(fishStockEventRepository, never()).save(any());
    }

    @Test
    void createStockEvent_shouldAllowDuplicateReference_whenTypeIsDifferent() {
        // Given
        FishStockEvent newEvent = FishStockEvent.builder()
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .type(FishStockEventType.SEED)
                .quantityDelta(1000)
                .referenceId("ref-1")
                .build();

        when(fishStockEventRepository.existsByTypeAndReferenceId(
                FishStockEventType.SEED, "ref-1")).thenReturn(false);
        when(fishStockEventRepository.save(any(FishStockEvent.class))).thenReturn(seedEvent);

        // When
        FishStockEvent result = fishStockEventService.createStockEvent(newEvent);

        // Then
        assertThat(result).isNotNull();
        verify(fishStockEventRepository).save(any(FishStockEvent.class));
    }

    @Test
    void getAllStockEvents_shouldReturnAllEvents() {
        // Given
        List<FishStockEvent> events = Arrays.asList(seedEvent, harvestEvent, mortalityEvent);
        when(fishStockEventRepository.findAll()).thenReturn(events);

        // When
        List<FishStockEvent> result = fishStockEventService.getAllStockEvents();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(seedEvent, harvestEvent, mortalityEvent);
        verify(fishStockEventRepository).findAll();
    }

    @Test
    void getAllStockEvents_shouldReturnEmpty_whenNoEvents() {
        // Given
        when(fishStockEventRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<FishStockEvent> result = fishStockEventService.getAllStockEvents();

        // Then
        assertThat(result).isEmpty();
        verify(fishStockEventRepository).findAll();
    }

    @Test
    void getStockEventsByBatch_shouldReturnEventsForBatch() {
        // Given
        List<FishStockEvent> events = Arrays.asList(seedEvent, harvestEvent, mortalityEvent);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        List<FishStockEvent> result = fishStockEventService.getStockEventsByBatch(BATCH_ID);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.stream().allMatch(e -> e.getBatchId().equals(BATCH_ID))).isTrue();
        verify(fishStockEventRepository).findByBatchId(BATCH_ID);
    }

    @Test
    void getStockEventsByPond_shouldReturnEventsForPond() {
        // Given
        List<FishStockEvent> events = Arrays.asList(seedEvent, harvestEvent);
        when(fishStockEventRepository.findByPondId(POND_ID)).thenReturn(events);

        // When
        List<FishStockEvent> result = fishStockEventService.getStockEventsByPond(POND_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().allMatch(e -> e.getPondId().equals(POND_ID))).isTrue();
        verify(fishStockEventRepository).findByPondId(POND_ID);
    }

    @Test
    void calculateCurrentStock_shouldSumQuantityDeltas() {
        // Given
        List<FishStockEvent> events = Arrays.asList(seedEvent, harvestEvent, mortalityEvent);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        Integer result = fishStockEventService.calculateCurrentStock(BATCH_ID);

        // Then
        // 10000 - 2000 - 500 = 7500
        assertThat(result).isEqualTo(7500);
        verify(fishStockEventRepository).findByBatchId(BATCH_ID);
    }

    @Test
    void calculateCurrentStock_shouldReturnZero_whenNoEvents() {
        // Given
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(Collections.emptyList());

        // When
        Integer result = fishStockEventService.calculateCurrentStock(BATCH_ID);

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    void calculateCurrentStock_shouldHandleNegativeStock() {
        // Given
        FishStockEvent largeHarvest = FishStockEvent.builder()
                .quantityDelta(-15000)
                .type(FishStockEventType.HARVEST)
                .build();
        List<FishStockEvent> events = Arrays.asList(seedEvent, largeHarvest);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        Integer result = fishStockEventService.calculateCurrentStock(BATCH_ID);

        // Then
        // 10000 - 15000 = -5000
        assertThat(result).isEqualTo(-5000);
    }

    @Test
    void calculatePondStock_shouldSumAllEventsInPond() {
        // Given
        List<FishStockEvent> events = Arrays.asList(seedEvent, harvestEvent, mortalityEvent);
        when(fishStockEventRepository.findByPondId(POND_ID)).thenReturn(events);

        // When
        Integer result = fishStockEventService.calculatePondStock(POND_ID);

        // Then
        assertThat(result).isEqualTo(7500);
        verify(fishStockEventRepository).findByPondId(POND_ID);
    }

    @Test
    void calculateStockBreakdown_shouldGroupByEventType() {
        // Given
        List<FishStockEvent> events = Arrays.asList(seedEvent, harvestEvent, mortalityEvent);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        Map<FishStockEventType, Integer> result = fishStockEventService.calculateStockBreakdown(BATCH_ID);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(FishStockEventType.SEED)).isEqualTo(10000);
        assertThat(result.get(FishStockEventType.HARVEST)).isEqualTo(-2000);
        assertThat(result.get(FishStockEventType.MORTALITY)).isEqualTo(-500);
    }

    @Test
    void calculateStockBreakdown_shouldCombineMultipleEventsOfSameType() {
        // Given
        FishStockEvent secondHarvest = FishStockEvent.builder()
                .quantityDelta(-1000)
                .type(FishStockEventType.HARVEST)
                .build();
        List<FishStockEvent> events = Arrays.asList(harvestEvent, secondHarvest);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        Map<FishStockEventType, Integer> result = fishStockEventService.calculateStockBreakdown(BATCH_ID);

        // Then
        assertThat(result.get(FishStockEventType.HARVEST)).isEqualTo(-3000);
    }

    @Test
    void calculateStockBreakdown_shouldReturnEmpty_whenNoEvents() {
        // Given
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(Collections.emptyList());

        // When
        Map<FishStockEventType, Integer> result = fishStockEventService.calculateStockBreakdown(BATCH_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void calculateEstimatedBiomass_shouldUseLatestWeight() {
        // Given - harvest has latest date with 150g avg weight
        List<FishStockEvent> events = Arrays.asList(seedEvent, mortalityEvent, harvestEvent);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        Double result = fishStockEventService.calculateEstimatedBiomass(BATCH_ID);

        // Then
        // Current stock: 10000 - 500 - 2000 = 7500
        // Latest weight: 150g (from harvest event)
        // Biomass: 7500 * 150 = 1,125,000g
        assertThat(result).isEqualTo(1_125_000.0);
    }

    @Test
    void calculateEstimatedBiomass_shouldReturnNull_whenNoWeightData() {
        // Given
        FishStockEvent eventWithoutWeight = FishStockEvent.builder()
                .quantityDelta(1000)
                .type(FishStockEventType.SEED)
                .avgWeightGram(null)
                .build();
        when(fishStockEventRepository.findByBatchId(BATCH_ID))
                .thenReturn(Collections.singletonList(eventWithoutWeight));

        // When
        Double result = fishStockEventService.calculateEstimatedBiomass(BATCH_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void calculateEstimatedBiomass_shouldReturnZero_whenNoCurrentStock() {
        // Given
        FishStockEvent completeHarvest = FishStockEvent.builder()
                .quantityDelta(-10000)
                .type(FishStockEventType.HARVEST)
                .avgWeightGram(150.0)
                .build();
        List<FishStockEvent> events = Arrays.asList(seedEvent, completeHarvest);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        Double result = fishStockEventService.calculateEstimatedBiomass(BATCH_ID);

        // Then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void calculateEstimatedBiomass_shouldIgnoreOlderWeights() {
        // Given
        FishStockEvent olderEvent = FishStockEvent.builder()
                .date(LocalDate.now().minusDays(60))
                .quantityDelta(5000)
                .type(FishStockEventType.SEED)
                .avgWeightGram(3.0)
                .build();
        // harvestEvent has date minusDays(5), so it's more recent
        List<FishStockEvent> events = Arrays.asList(olderEvent, seedEvent, harvestEvent);
        when(fishStockEventRepository.findByBatchId(BATCH_ID)).thenReturn(events);

        // When
        Double result = fishStockEventService.calculateEstimatedBiomass(BATCH_ID);

        // Then
        // Should use harvestEvent's weight (150g), not olderEvent's (3g)
        // Current stock: 5000 + 10000 - 2000 = 13000
        // Biomass: 13000 * 150 = 1,950,000g
        assertThat(result).isEqualTo(1_950_000.0);
    }

    @Test
    void stockEventExists_shouldReturnTrue_whenEventExists() {
        // Given
        when(fishStockEventRepository.existsByTypeAndReferenceId(
                FishStockEventType.HARVEST, "harvest-1")).thenReturn(true);

        // When
        boolean result = fishStockEventService.stockEventExists(
                FishStockEventType.HARVEST, "harvest-1");

        // Then
        assertThat(result).isTrue();
        verify(fishStockEventRepository).existsByTypeAndReferenceId(
                FishStockEventType.HARVEST, "harvest-1");
    }

    @Test
    void stockEventExists_shouldReturnFalse_whenEventDoesNotExist() {
        // Given
        when(fishStockEventRepository.existsByTypeAndReferenceId(
                FishStockEventType.HARVEST, "harvest-999")).thenReturn(false);

        // When
        boolean result = fishStockEventService.stockEventExists(
                FishStockEventType.HARVEST, "harvest-999");

        // Then
        assertThat(result).isFalse();
    }
}
