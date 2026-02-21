package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishSeed;
import com.aquagrow.api.model.FishType;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.FishTypeRepository;
import com.aquagrow.api.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FishTypeServiceTest {

    @Mock
    private FishTypeRepository fishTypeRepository;

    @Mock
    private BatchRepository batchRepository;

    @InjectMocks
    private FishTypeService fishTypeService;

    private FishType testFishType;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-123");

        testFishType = FishType.builder()
                .id("fish-1")
                .name("Tilapia")
                .scientificName("Oreochromis niloticus")
                .tenantId("tenant-123")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==================== Create Tests ====================

    @Test
    void createFishType_shouldSetTenantIdAndSave() {
        // Given
        FishType newFishType = FishType.builder()
                .name("Catfish")
                .scientificName("Clarias gariepinus")
                .build();

        when(fishTypeRepository.findByName("Catfish")).thenReturn(Optional.empty());
        when(fishTypeRepository.save(any(FishType.class))).thenReturn(testFishType);

        // When
        FishType result = fishTypeService.createFishType(newFishType);

        // Then
        assertThat(result).isNotNull();
        verify(fishTypeRepository).save(argThat(ft -> ft.getTenantId().equals("tenant-123")));
    }

    @Test
    void createFishType_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();
        FishType newFishType = FishType.builder().name("Catfish").build();

        // When/Then
        assertThatThrownBy(() -> fishTypeService.createFishType(newFishType))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    void createFishType_shouldThrowException_whenNameMissing() {
        // Given
        FishType newFishType = FishType.builder().build();

        // When/Then
        assertThatThrownBy(() -> fishTypeService.createFishType(newFishType))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fish type name is required");
    }

    @Test
    void createFishType_shouldThrowException_whenNameTooLong() {
        // Given
        FishType newFishType = FishType.builder()
                .name("A".repeat(101))
                .build();

        // When/Then
        assertThatThrownBy(() -> fishTypeService.createFishType(newFishType))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must not exceed 100 characters");
    }

    @Test
    void createFishType_shouldThrowException_whenDuplicateName() {
        // Given
        FishType newFishType = FishType.builder().name("Tilapia").build();
        when(fishTypeRepository.findByName("Tilapia")).thenReturn(Optional.of(testFishType));

        // When/Then
        assertThatThrownBy(() -> fishTypeService.createFishType(newFishType))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    // ==================== Read Tests ====================

    @Test
    void getAllFishTypes_shouldReturnAllForTenant() {
        // Given
        when(fishTypeRepository.findByTenantId("tenant-123"))
                .thenReturn(Arrays.asList(testFishType));

        // When
        List<FishType> result = fishTypeService.getAllFishTypes();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Tilapia");
    }

    @Test
    void getAllFishTypes_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();

        // When/Then
        assertThatThrownBy(() -> fishTypeService.getAllFishTypes())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    void getFishTypeById_shouldReturnFishType() {
        // Given
        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.of(testFishType));

        // When
        Optional<FishType> result = fishTypeService.getFishTypeById("fish-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tilapia");
    }

    @Test
    void getFishTypeByName_shouldReturnFishType() {
        // Given
        when(fishTypeRepository.findByName("Tilapia")).thenReturn(Optional.of(testFishType));

        // When
        Optional<FishType> result = fishTypeService.getFishTypeByName("Tilapia");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tilapia");
    }

    // ==================== Update Tests ====================

    @Test
    void updateFishType_shouldUpdateFields() {
        // Given
        FishType updates = FishType.builder()
                .name("Nile Tilapia")
                .scientificName("O. niloticus")
                .build();

        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.of(testFishType));
        when(fishTypeRepository.findByName("Nile Tilapia")).thenReturn(Optional.empty());
        when(fishTypeRepository.save(any(FishType.class))).thenReturn(testFishType);

        // When
        FishType result = fishTypeService.updateFishType("fish-1", updates);

        // Then
        verify(fishTypeRepository).save(argThat(ft ->
                ft.getName().equals("Nile Tilapia") &&
                ft.getScientificName().equals("O. niloticus")
        ));
    }

    @Test
    void updateFishType_shouldThrowException_whenNotFound() {
        // Given
        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fishTypeService.updateFishType("fish-1", testFishType))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fish type not found");
    }

    @Test
    void updateFishType_shouldThrowException_whenWrongTenant() {
        // Given
        FishType otherTenantFish = FishType.builder()
                .id("fish-1")
                .name("Tilapia")
                .tenantId("other-tenant")
                .build();

        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.of(otherTenantFish));

        // When/Then
        assertThatThrownBy(() -> fishTypeService.updateFishType("fish-1", testFishType))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fish type not found");
    }

    @Test
    void updateFishType_shouldThrowException_whenDuplicateName() {
        // Given
        FishType existing = FishType.builder()
                .id("fish-1")
                .name("Tilapia")
                .tenantId("tenant-123")
                .build();

        FishType duplicate = FishType.builder()
                .id("fish-2")
                .name("Catfish")
                .tenantId("tenant-123")
                .build();

        FishType updates = FishType.builder().name("Catfish").build();

        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.of(existing));
        when(fishTypeRepository.findByName("Catfish")).thenReturn(Optional.of(duplicate));

        // When/Then
        assertThatThrownBy(() -> fishTypeService.updateFishType("fish-1", updates))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    // ==================== Delete Tests ====================

    @Test
    void deleteFishType_shouldDeleteSuccessfully() {
        // Given
        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.of(testFishType));
        when(batchRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        fishTypeService.deleteFishType("fish-1");

        // Then
        verify(fishTypeRepository).deleteById("fish-1");
    }

    @Test
    void deleteFishType_shouldThrowException_whenNotFound() {
        // Given
        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fishTypeService.deleteFishType("fish-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fish type not found");
    }

    @Test
    void deleteFishType_shouldThrowException_whenInUse() {
        // Given
        FishSeed seed = FishSeed.builder()
                .fishTypeId("fish-1")
                .build();

        Batch batch = Batch.builder()
                .id("batch-1")
                .fishSeeds(Arrays.asList(seed))
                .build();

        when(fishTypeRepository.findById("fish-1")).thenReturn(Optional.of(testFishType));
        when(batchRepository.findAll()).thenReturn(Arrays.asList(batch));

        // When/Then
        assertThatThrownBy(() -> fishTypeService.deleteFishType("fish-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete fish type that is in use");
    }

    // ==================== Analytics Tests ====================

    @Test
    void isInUse_shouldReturnTrue_whenUsed() {
        // Given
        FishSeed seed = FishSeed.builder()
                .fishTypeId("fish-1")
                .build();

        Batch batch = Batch.builder()
                .fishSeeds(Arrays.asList(seed))
                .build();

        when(batchRepository.findAll()).thenReturn(Arrays.asList(batch));

        // When
        boolean result = fishTypeService.isInUse("fish-1");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isInUse_shouldReturnFalse_whenNotUsed() {
        // Given
        when(batchRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        boolean result = fishTypeService.isInUse("fish-1");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getBatchCountForFishType_shouldReturnCorrectCount() {
        // Given
        FishSeed seed1 = FishSeed.builder().fishTypeId("fish-1").build();
        FishSeed seed2 = FishSeed.builder().fishTypeId("fish-1").build();

        Batch batch1 = Batch.builder().fishSeeds(Arrays.asList(seed1)).build();
        Batch batch2 = Batch.builder().fishSeeds(Arrays.asList(seed2)).build();

        when(batchRepository.findAll()).thenReturn(Arrays.asList(batch1, batch2));

        // When
        long count = fishTypeService.getBatchCountForFishType("fish-1");

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void getBatchesForFishType_shouldReturnBatches() {
        // Given
        FishSeed seed = FishSeed.builder().fishTypeId("fish-1").build();
        Batch batch = Batch.builder().id("batch-1").fishSeeds(Arrays.asList(seed)).build();

        when(batchRepository.findAll()).thenReturn(Arrays.asList(batch));

        // When
        List<Batch> batches = fishTypeService.getBatchesForFishType("fish-1");

        // Then
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).getId()).isEqualTo("batch-1");
    }

    @Test
    void searchByName_shouldReturnMatchingFishTypes() {
        // Given
        FishType tilapia = FishType.builder().name("Tilapia").tenantId("tenant-123").build();
        FishType catfish = FishType.builder().name("Catfish").tenantId("tenant-123").build();

        when(fishTypeRepository.findByTenantId("tenant-123"))
                .thenReturn(Arrays.asList(tilapia, catfish));

        // When
        List<FishType> results = fishTypeService.searchByName("til");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Tilapia");
    }

    @Test
    void searchByName_shouldBeCaseInsensitive() {
        // Given
        FishType tilapia = FishType.builder().name("Tilapia").tenantId("tenant-123").build();

        when(fishTypeRepository.findByTenantId("tenant-123"))
                .thenReturn(Arrays.asList(tilapia));

        // When
        List<FishType> results = fishTypeService.searchByName("TILA");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Tilapia");
    }
}
