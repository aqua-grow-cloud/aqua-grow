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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BatchService.
 * Tests business logic in isolation using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BatchService Tests")
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private PondRepository pondRepository;

    @Mock
    private FishStockEventRepository fishStockEventRepository;

    @Mock
    private FishTypeRepository fishTypeRepository;

    @InjectMocks
    private BatchService batchService;

    private static final String TEST_TENANT_ID = "tenant-123";
    private static final String TEST_BATCH_ID = "batch-456";
    private static final String TEST_POND_ID = "pond-789";
    private static final String TEST_FISH_TYPE_ID = "fishtype-101";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TEST_TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========== CREATE BATCH TESTS ==========

    @Test
    @DisplayName("createBatch - should set tenant ID and save batch")
    void createBatch_shouldSetTenantIdAndSave() {
        // Given
        Batch batch = Batch.builder()
                .name("Test Batch")
                .stockingDate(LocalDate.now())
                .build();

        Batch savedBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .name("Test Batch")
                .tenantId(TEST_TENANT_ID)
                .stockingDate(LocalDate.now())
                .fishSeeds(new ArrayList<>())
                .build();

        when(batchRepository.save(any(Batch.class))).thenReturn(savedBatch);

        // When
        Batch result = batchService.createBatch(batch);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(TEST_TENANT_ID);
        verify(batchRepository).save(batch);
        assertThat(batch.getTenantId()).isEqualTo(TEST_TENANT_ID);
    }

    @Test
    @DisplayName("createBatch - should throw exception when tenant context is null")
    void createBatch_shouldThrowExceptionWhenTenantContextIsNull() {
        // Given
        TenantContext.clear();
        Batch batch = Batch.builder().name("Test Batch").build();

        // When / Then
        assertThatThrownBy(() -> batchService.createBatch(batch))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    @DisplayName("createBatch - should create stock events for fish seeds")
    void createBatch_shouldCreateStockEventsForFishSeeds() {
        // Given
        FishSeed seed = FishSeed.builder()
                .id("seed-1")
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .quantity(1000)
                .sizeGrams(10.0)
                .date(LocalDate.now())
                .build();

        Batch batch = Batch.builder()
                .name("Test Batch")
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        Batch savedBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .name("Test Batch")
                .tenantId(TEST_TENANT_ID)
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        when(batchRepository.save(any(Batch.class))).thenReturn(savedBatch);

        // When
        batchService.createBatch(batch);

        // Then
        ArgumentCaptor<FishStockEvent> eventCaptor = ArgumentCaptor.forClass(FishStockEvent.class);
        verify(fishStockEventRepository).save(eventCaptor.capture());

        FishStockEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getBatchId()).isEqualTo(TEST_BATCH_ID);
        assertThat(capturedEvent.getType()).isEqualTo(FishStockEventType.SEED);
        assertThat(capturedEvent.getQuantityDelta()).isEqualTo(1000);
        assertThat(capturedEvent.getTenantId()).isEqualTo(TEST_TENANT_ID);
    }

    @Test
    @DisplayName("createBatch - should update pond batch assignment")
    void createBatch_shouldUpdatePondBatchAssignment() {
        // Given
        FishSeed seed = FishSeed.builder()
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .quantity(1000)
                .build();

        Batch batch = Batch.builder()
                .name("Test Batch")
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        Batch savedBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        Pond pond = Pond.builder()
                .id(TEST_POND_ID)
                .name("Test Pond")
                .build();

        when(batchRepository.save(any(Batch.class))).thenReturn(savedBatch);
        when(pondRepository.findById(TEST_POND_ID)).thenReturn(Optional.of(pond));

        // When
        batchService.createBatch(batch);

        // Then
        verify(pondRepository).findById(TEST_POND_ID);
        assertThat(pond.getCurrentBatchId()).isEqualTo(TEST_BATCH_ID);
        verify(pondRepository).save(pond);
    }

    // ========== GET BATCH TESTS ==========

    @Test
    @DisplayName("getBatchById - should return batch when found")
    void getBatchById_shouldReturnBatchWhenFound() {
        // Given
        Batch batch = Batch.builder().id(TEST_BATCH_ID).name("Test Batch").build();
        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));

        // When
        Optional<Batch> result = batchService.getBatchById(TEST_BATCH_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_BATCH_ID);
    }

    @Test
    @DisplayName("getBatchById - should return empty when not found")
    void getBatchById_shouldReturnEmptyWhenNotFound() {
        // Given
        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.empty());

        // When
        Optional<Batch> result = batchService.getBatchById(TEST_BATCH_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getBatchesByPond - should return batches for pond")
    void getBatchesByPond_shouldReturnBatchesForPond() {
        // Given
        List<Batch> batches = List.of(
                Batch.builder().id("batch-1").build(),
                Batch.builder().id("batch-2").build()
        );
        when(batchRepository.findDistinctByFishSeedsPondId(TEST_POND_ID)).thenReturn(batches);

        // When
        List<Batch> result = batchService.getBatchesByPond(TEST_POND_ID);

        // Then
        assertThat(result).hasSize(2);
        verify(batchRepository).findDistinctByFishSeedsPondId(TEST_POND_ID);
    }

    @Test
    @DisplayName("getAllBatches - should apply name filter")
    void getAllBatches_shouldApplyNameFilter() {
        // Given
        List<Batch> batches = List.of(Batch.builder().name("Batch A").build());
        when(batchRepository.findAll(any(Specification.class))).thenReturn(batches);

        // When
        List<Batch> result = batchService.getAllBatches("Batch A", null, null);

        // Then
        assertThat(result).hasSize(1);
        verify(batchRepository).findAll(any(Specification.class));
    }

    // ========== UPDATE BATCH TESTS ==========

    @Test
    @DisplayName("updateBatch - should update batch details")
    void updateBatch_shouldUpdateBatchDetails() {
        // Given
        Batch existingBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .name("Old Name")
                .stockingDate(LocalDate.of(2024, 1, 1))
                .build();

        Batch updates = Batch.builder()
                .name("New Name")
                .stockingDate(LocalDate.of(2024, 2, 1))
                .estimatedHarvestDate(LocalDate.of(2024, 12, 1))
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(existingBatch));
        when(batchRepository.save(any(Batch.class))).thenReturn(existingBatch);

        // When
        Optional<Batch> result = batchService.updateBatch(TEST_BATCH_ID, updates);

        // Then
        assertThat(result).isPresent();
        assertThat(existingBatch.getName()).isEqualTo("New Name");
        assertThat(existingBatch.getStockingDate()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(existingBatch.getEstimatedHarvestDate()).isEqualTo(LocalDate.of(2024, 12, 1));
        verify(batchRepository).save(existingBatch);
    }

    @Test
    @DisplayName("updateBatch - should return empty when batch not found")
    void updateBatch_shouldReturnEmptyWhenBatchNotFound() {
        // Given
        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.empty());
        Batch updates = Batch.builder().name("New Name").build();

        // When
        Optional<Batch> result = batchService.updateBatch(TEST_BATCH_ID, updates);

        // Then
        assertThat(result).isEmpty();
        verify(batchRepository, never()).save(any());
    }

    // ========== ADD FISH SEED TESTS ==========

    @Test
    @DisplayName("addFishSeed - should add seed to batch")
    void addFishSeed_shouldAddSeedToBatch() {
        // Given
        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .tenantId(TEST_TENANT_ID)
                .fishSeeds(new ArrayList<>())
                .build();

        FishSeed newSeed = FishSeed.builder()
                .id("temp-id")
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .quantity(500)
                .date(LocalDate.now())
                .build();

        FishSeed savedSeed = FishSeed.builder()
                .id("saved-seed-id")
                .pondId(TEST_POND_ID)
                .fishTypeId(TEST_FISH_TYPE_ID)
                .quantity(500)
                .tenantId(TEST_TENANT_ID)
                .status("Active")
                .build();

        Batch savedBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .tenantId(TEST_TENANT_ID)
                .fishSeeds(new ArrayList<>(List.of(savedSeed)))
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(savedBatch);

        // When
        Optional<Batch> result = batchService.addFishSeed(TEST_BATCH_ID, newSeed);

        // Then
        assertThat(result).isPresent();
        assertThat(batch.getFishSeeds()).hasSize(1);
        assertThat(newSeed.getId()).isNull(); // ID should be cleared
        assertThat(newSeed.getTenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(newSeed.getStatus()).isEqualTo("Active");
        verify(fishStockEventRepository).save(any(FishStockEvent.class));
    }

    // ========== UPDATE STATUS TESTS ==========

    @Test
    @DisplayName("updateSeedStatus - should update specific seed status")
    void updateSeedStatus_shouldUpdateSpecificSeedStatus() {
        // Given
        FishSeed seed1 = FishSeed.builder().id("seed-1").status("Active").build();
        FishSeed seed2 = FishSeed.builder().id("seed-2").status("Active").build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed1, seed2)))
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);

        // When
        Optional<Batch> result = batchService.updateSeedStatus(TEST_BATCH_ID, "seed-1", "Harvested");

        // Then
        assertThat(result).isPresent();
        assertThat(seed1.getStatus()).isEqualTo("Harvested");
        assertThat(seed2.getStatus()).isEqualTo("Active"); // Unchanged
        verify(batchRepository).save(batch);
    }

    @Test
    @DisplayName("updateBatchStatus - should update all seeds status")
    void updateBatchStatus_shouldUpdateAllSeedsStatus() {
        // Given
        FishSeed seed1 = FishSeed.builder().id("seed-1").status("Active").build();
        FishSeed seed2 = FishSeed.builder().id("seed-2").status("Active").build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed1, seed2)))
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);

        // When
        Optional<Batch> result = batchService.updateBatchStatus(TEST_BATCH_ID, "Completed");

        // Then
        assertThat(result).isPresent();
        assertThat(seed1.getStatus()).isEqualTo("Completed");
        assertThat(seed2.getStatus()).isEqualTo("Completed");
        verify(batchRepository).save(batch);
    }

    // ========== MOVE FISH SEED TESTS ==========

    @Test
    @DisplayName("moveFishSeed - should move full quantity to new pond")
    void moveFishSeed_shouldMoveFullQuantityToNewPond() {
        // Given
        FishSeed seed = FishSeed.builder()
                .id("seed-1")
                .pondId("pond-A")
                .fishTypeId(TEST_FISH_TYPE_ID)
                .quantity(1000)
                .sizeGrams(15.0)
                .status("Active")
                .build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);

        // When
        Optional<Batch> result = batchService.moveFishSeed(TEST_BATCH_ID, "seed-1", "pond-B", null);

        // Then
        assertThat(result).isPresent();
        assertThat(seed.getPondId()).isEqualTo("pond-B");
        assertThat(seed.getQuantity()).isEqualTo(1000); // Full quantity moved
        verify(fishStockEventRepository, times(2)).save(any(FishStockEvent.class)); // OUT + IN events
    }

    @Test
    @DisplayName("moveFishSeed - should split seed for partial move")
    void moveFishSeed_shouldSplitSeedForPartialMove() {
        // Given
        FishSeed seed = FishSeed.builder()
                .id("seed-1")
                .pondId("pond-A")
                .fishTypeId(TEST_FISH_TYPE_ID)
                .quantity(1000)
                .sizeGrams(15.0)
                .build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);

        // When
        Optional<Batch> result = batchService.moveFishSeed(TEST_BATCH_ID, "seed-1", "pond-B", 300);

        // Then
        assertThat(result).isPresent();
        assertThat(seed.getQuantity()).isEqualTo(700); // Remaining in original pond
        assertThat(batch.getFishSeeds()).hasSize(2); // New seed created
        assertThat(batch.getFishSeeds().get(1).getQuantity()).isEqualTo(300);
        assertThat(batch.getFishSeeds().get(1).getPondId()).isEqualTo("pond-B");
    }

    @Test
    @DisplayName("moveFishSeed - should throw exception for invalid quantity")
    void moveFishSeed_shouldThrowExceptionForInvalidQuantity() {
        // Given
        FishSeed seed = FishSeed.builder()
                .id("seed-1")
                .quantity(1000)
                .build();

        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed)))
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));

        // When / Then
        assertThatThrownBy(() -> batchService.moveFishSeed(TEST_BATCH_ID, "seed-1", "pond-B", 1500))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid move quantity");
    }

    @Test
    @DisplayName("moveFishSeed - should throw exception when seed not found")
    void moveFishSeed_shouldThrowExceptionWhenSeedNotFound() {
        // Given
        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>())
                .build();

        when(batchRepository.findById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));

        // When / Then
        assertThatThrownBy(() -> batchService.moveFishSeed(TEST_BATCH_ID, "non-existent", "pond-B", 100))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fish seed not found");
    }

    // ========== TENANT ISOLATION TESTS ==========

    @Test
    @DisplayName("createBatch - should set tenant ID on all fish seeds")
    void createBatch_shouldSetTenantIdOnAllFishSeeds() {
        // Given
        FishSeed seed1 = FishSeed.builder().fishTypeId("type-1").build();
        FishSeed seed2 = FishSeed.builder().fishTypeId("type-2").build();

        Batch batch = Batch.builder()
                .name("Test Batch")
                .fishSeeds(new ArrayList<>(List.of(seed1, seed2)))
                .build();

        Batch savedBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(seed1, seed2)))
                .build();

        when(batchRepository.save(any(Batch.class))).thenReturn(savedBatch);

        // When
        batchService.createBatch(batch);

        // Then
        assertThat(seed1.getTenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(seed2.getTenantId()).isEqualTo(TEST_TENANT_ID);
    }
}
