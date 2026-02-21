package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Pond;
import com.aquagrow.api.repository.PondRepository;
import com.aquagrow.api.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PondServiceTest {

    @Mock
    private PondRepository pondRepository;

    @InjectMocks
    private PondService pondService;

    private static final String TENANT_ID = "tenant-123";
    private static final String POND_ID = "pond-1";

    private Pond testPond;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        testPond = Pond.builder()
                .id(POND_ID)
                .name("Test Pond")
                .area(1000.0) // 1000 m²
                .depth(2.0) // 2 m
                .type("intensive")
                .status("Active")
                .tenantId(TENANT_ID)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPond_shouldSetTenantId() {
        // Given
        Pond newPond = Pond.builder()
                .name("New Pond")
                .area(500.0)
                .status("Active")
                .build();

        when(pondRepository.save(any(Pond.class))).thenReturn(testPond);

        // When
        Pond result = pondService.createPond(newPond);

        // Then
        ArgumentCaptor<Pond> captor = ArgumentCaptor.forClass(Pond.class);
        verify(pondRepository).save(captor.capture());

        Pond savedPond = captor.getValue();
        assertThat(savedPond.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result).isEqualTo(testPond);
    }

    @Test
    void createPond_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();
        Pond newPond = Pond.builder().name("New Pond").status("Active").build();

        // When & Then
        assertThatThrownBy(() -> pondService.createPond(newPond))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant context is required");

        verify(pondRepository, never()).save(any());
    }

    @Test
    void createPond_shouldThrowException_whenNameIsMissing() {
        // Given
        Pond newPond = Pond.builder().status("Active").build();

        // When & Then
        assertThatThrownBy(() -> pondService.createPond(newPond))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Pond name is required");
    }

    @Test
    void createPond_shouldThrowException_whenStatusIsMissing() {
        // Given
        Pond newPond = Pond.builder().name("New Pond").build();

        // When & Then
        assertThatThrownBy(() -> pondService.createPond(newPond))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Pond status is required");
    }

    @Test
    void createPond_shouldThrowException_whenAreaIsNegative() {
        // Given
        Pond newPond = Pond.builder()
                .name("New Pond")
                .status("Active")
                .area(-100.0)
                .build();

        // When & Then
        assertThatThrownBy(() -> pondService.createPond(newPond))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Pond area must be greater than 0");
    }

    @Test
    void getAllPonds_shouldReturnAllPonds() {
        // Given
        Pond pond2 = Pond.builder().id("pond-2").name("Pond 2").build();
        when(pondRepository.findAll()).thenReturn(Arrays.asList(testPond, pond2));

        // When
        List<Pond> result = pondService.getAllPonds();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testPond, pond2);
    }

    @Test
    void getPondById_shouldReturnPond() {
        // Given
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Pond result = pondService.getPondById(POND_ID);

        // Then
        assertThat(result).isEqualTo(testPond);
    }

    @Test
    void getPondById_shouldThrowException_whenNotFound() {
        // Given
        when(pondRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pondService.getPondById("non-existent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pond not found");
    }

    @Test
    void updatePond_shouldUpdateAllFields() {
        // Given
        Pond updates = Pond.builder()
                .name("Updated Name")
                .area(1500.0)
                .depth(3.0)
                .type("extensive")
                .status("Maintenance")
                .currentBatchId("batch-123")
                .latitude(10.5)
                .longitude(20.5)
                .build();

        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));
        when(pondRepository.save(any(Pond.class))).thenReturn(testPond);

        // When
        Pond result = pondService.updatePond(POND_ID, updates);

        // Then
        assertThat(testPond.getName()).isEqualTo("Updated Name");
        assertThat(testPond.getArea()).isEqualTo(1500.0);
        assertThat(testPond.getDepth()).isEqualTo(3.0);
        assertThat(testPond.getType()).isEqualTo("extensive");
        assertThat(testPond.getStatus()).isEqualTo("Maintenance");
    }

    @Test
    void deletePond_shouldDeletePond() {
        // Given
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        pondService.deletePond(POND_ID);

        // Then
        verify(pondRepository).delete(testPond);
    }

    @Test
    void deletePond_shouldThrowException_whenPondHasActiveBatch() {
        // Given
        testPond.setCurrentBatchId("active-batch");
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When & Then
        assertThatThrownBy(() -> pondService.deletePond(POND_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete pond")
                .hasMessageContaining("has an active batch");

        verify(pondRepository, never()).delete(any());
    }

    @Test
    void calculateWaterVolume_shouldCalculateCorrectly() {
        // Given
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Double result = pondService.calculateWaterVolume(POND_ID);

        // Then
        // Volume = Area × Depth = 1000 × 2 = 2000 m³
        assertThat(result).isEqualTo(2000.0);
    }

    @Test
    void calculateWaterVolume_shouldReturnNull_whenAreaMissing() {
        // Given
        testPond.setArea(null);
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Double result = pondService.calculateWaterVolume(POND_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void calculateMaxStockingCapacity_shouldUseIntensiveDensity() {
        // Given
        testPond.setType("intensive");
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Integer result = pondService.calculateMaxStockingCapacity(POND_ID);

        // Then
        // Capacity = Area × Density = 1000 × 20 = 20000 fish
        assertThat(result).isEqualTo(20000);
    }

    @Test
    void calculateMaxStockingCapacity_shouldUseExtensiveDensity() {
        // Given
        testPond.setType("extensive");
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Integer result = pondService.calculateMaxStockingCapacity(POND_ID);

        // Then
        // Capacity = Area × Density = 1000 × 5 = 5000 fish
        assertThat(result).isEqualTo(5000);
    }

    @Test
    void calculateMaxStockingCapacity_shouldUseDefaultDensity_whenTypeIsNull() {
        // Given
        testPond.setType(null);
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Integer result = pondService.calculateMaxStockingCapacity(POND_ID);

        // Then
        // Capacity = Area × Density = 1000 × 10 = 10000 fish
        assertThat(result).isEqualTo(10000);
    }

    @Test
    void calculateCurrentStockingDensity_shouldCalculateCorrectly() {
        // Given
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Double result = pondService.calculateCurrentStockingDensity(POND_ID, 5000);

        // Then
        // Density = Stock / Area = 5000 / 1000 = 5.0 fish/m²
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    void calculateCurrentStockingDensity_shouldReturnZero_whenStockIsNull() {
        // Given
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        Double result = pondService.calculateCurrentStockingDensity(POND_ID, null);

        // Then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void canAccommodateFish_shouldReturnTrue_whenWithinCapacity() {
        // Given
        testPond.setType("intensive"); // 20 fish/m² × 1000 m² = 20000 capacity
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        boolean result = pondService.canAccommodateFish(POND_ID, 10000, 5000);

        // Then
        // Total: 10000 + 5000 = 15000 <= 20000
        assertThat(result).isTrue();
    }

    @Test
    void canAccommodateFish_shouldReturnFalse_whenExceedsCapacity() {
        // Given
        testPond.setType("intensive"); // 20000 capacity
        when(pondRepository.findById(POND_ID)).thenReturn(Optional.of(testPond));

        // When
        boolean result = pondService.canAccommodateFish(POND_ID, 18000, 5000);

        // Then
        // Total: 18000 + 5000 = 23000 > 20000
        assertThat(result).isFalse();
    }

    @Test
    void getPondsByStatus_shouldFilterByStatus() {
        // Given
        Pond activePond = Pond.builder().status("Active").build();
        Pond maintenancePond = Pond.builder().status("Maintenance").build();
        when(pondRepository.findAll()).thenReturn(Arrays.asList(activePond, maintenancePond, testPond));

        // When
        List<Pond> result = pondService.getPondsByStatus("Active");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().allMatch(p -> "Active".equals(p.getStatus()))).isTrue();
    }

    @Test
    void getActivePonds_shouldReturnOnlyActivePonds() {
        // Given
        Pond activePond = Pond.builder().status("Active").build();
        Pond inactivePond = Pond.builder().status("Inactive").build();
        when(pondRepository.findAll()).thenReturn(Arrays.asList(testPond, activePond, inactivePond));

        // When
        List<Pond> result = pondService.getActivePonds();

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void getAvailablePonds_shouldReturnActiveWithoutBatch() {
        // Given
        Pond available = Pond.builder().status("Active").currentBatchId(null).build();
        Pond occupied = Pond.builder().status("Active").currentBatchId("batch-1").build();
        Pond inactive = Pond.builder().status("Inactive").currentBatchId(null).build();
        when(pondRepository.findAll()).thenReturn(Arrays.asList(available, occupied, inactive));

        // When
        List<Pond> result = pondService.getAvailablePonds();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(available);
    }

    @Test
    void calculateTotalPondArea_shouldSumAllAreas() {
        // Given
        Pond pond1 = Pond.builder().area(1000.0).build();
        Pond pond2 = Pond.builder().area(1500.0).build();
        Pond pond3 = Pond.builder().area(2000.0).build();
        when(pondRepository.findAll()).thenReturn(Arrays.asList(pond1, pond2, pond3));

        // When
        Double result = pondService.calculateTotalPondArea();

        // Then
        assertThat(result).isEqualTo(4500.0);
    }

    @Test
    void calculatePondUtilization_shouldCalculateCorrectPercentage() {
        // Given
        Pond occupied1 = Pond.builder().status("Active").currentBatchId("batch-1").build();
        Pond occupied2 = Pond.builder().status("Active").currentBatchId("batch-2").build();
        Pond available = Pond.builder().status("Active").currentBatchId(null).build();
        Pond inactive = Pond.builder().status("Inactive").currentBatchId(null).build();
        when(pondRepository.findAll()).thenReturn(Arrays.asList(occupied1, occupied2, available, inactive));

        // When
        Double result = pondService.calculatePondUtilization();

        // Then
        // 2 occupied / 3 active = 66.67%
        assertThat(result).isCloseTo(66.67, within(0.01));
    }

    @Test
    void calculatePondUtilization_shouldReturnZero_whenNoActivePonds() {
        // Given
        Pond inactive = Pond.builder().status("Inactive").build();
        when(pondRepository.findAll()).thenReturn(Collections.singletonList(inactive));

        // When
        Double result = pondService.calculatePondUtilization();

        // Then
        assertThat(result).isEqualTo(0.0);
    }
}
