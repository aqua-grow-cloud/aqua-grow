package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.*;
import com.aquagrow.api.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SampleServiceTest {

    @Mock
    private SampleLogRepository sampleLogRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private PondRepository pondRepository;

    @Mock
    private FishTypeRepository fishTypeRepository;

    @Mock
    private FishStockEventRepository fishStockEventRepository;

    @InjectMocks
    private SampleService sampleService;

    private SampleLog testSample;
    private Batch testBatch;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-123");

        testBatch = Batch.builder()
                .id("batch-1")
                .name("Batch A")
                .tenantId("tenant-123")
                .fishSeeds(new ArrayList<>())
                .build();

        testSample = SampleLog.builder()
                .id("sample-1")
                .batchId("batch-1")
                .fishTypeId("fish-1")
                .date(LocalDate.now())
                .sampleCount(10)
                .totalSampleWeight(500.0)
                .averageWeight(50.0)
                .tenantId("tenant-123")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==================== Create Tests ====================

    @Test
    void createSampleLog_shouldSetTenantIdAndSave() {
        // Given
        SampleLog newSample = SampleLog.builder()
                .batchId("batch-1")
                .date(LocalDate.now())
                .sampleCount(10)
                .averageWeight(50.0)
                .build();

        when(sampleLogRepository.save(any(SampleLog.class))).thenReturn(testSample);
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(testBatch));
        when(fishStockEventRepository.save(any(FishStockEvent.class))).thenReturn(new FishStockEvent());

        // When
        SampleLog result = sampleService.createSampleLog(newSample);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo("tenant-123");
        verify(sampleLogRepository).save(argThat(sample -> sample.getTenantId().equals("tenant-123")));
        verify(fishStockEventRepository).save(any(FishStockEvent.class));
    }

    @Test
    void createSampleLog_shouldThrowException_whenTenantContextMissing() {
        // Given
        TenantContext.clear();
        SampleLog newSample = SampleLog.builder().batchId("batch-1").date(LocalDate.now()).build();

        // When/Then
        assertThatThrownBy(() -> sampleService.createSampleLog(newSample))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    void createSampleLog_shouldThrowException_whenBatchIdMissing() {
        // Given
        SampleLog newSample = SampleLog.builder().date(LocalDate.now()).build();

        // When/Then
        assertThatThrownBy(() -> sampleService.createSampleLog(newSample))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Batch ID is required");
    }

    @Test
    void createSampleLog_shouldThrowException_whenDateMissing() {
        // Given
        SampleLog newSample = SampleLog.builder().batchId("batch-1").build();

        // When/Then
        assertThatThrownBy(() -> sampleService.createSampleLog(newSample))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sample date is required");
    }

    @Test
    void createSampleLog_shouldThrowException_whenSampleCountInvalid() {
        // Given
        SampleLog newSample = SampleLog.builder()
                .batchId("batch-1")
                .date(LocalDate.now())
                .sampleCount(0)
                .build();

        // When/Then
        assertThatThrownBy(() -> sampleService.createSampleLog(newSample))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sample count must be greater than zero");
    }

    @Test
    void createSampleLog_shouldThrowException_whenAverageWeightInvalid() {
        // Given
        SampleLog newSample = SampleLog.builder()
                .batchId("batch-1")
                .date(LocalDate.now())
                .averageWeight(-10.0)
                .build();

        // When/Then
        assertThatThrownBy(() -> sampleService.createSampleLog(newSample))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Average weight must be greater than zero");
    }

    @Test
    void createSampleLog_shouldThrowException_whenSurvivalEstimateInvalid() {
        // Given
        SampleLog newSample = SampleLog.builder()
                .batchId("batch-1")
                .date(LocalDate.now())
                .survivalEstimate(150.0)
                .build();

        // When/Then
        assertThatThrownBy(() -> sampleService.createSampleLog(newSample))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Survival estimate must be between 0 and 100");
    }

    // ==================== Read Tests ====================

    @Test
    void getAllSampleLogs_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<SampleLog> samples = Arrays.asList(testSample);
        Page<SampleLog> page = new PageImpl<>(samples, pageable, samples.size());

        when(sampleLogRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<SampleLog> result = sampleService.getAllSampleLogs(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getSampleLogById_shouldReturnSampleLog() {
        // Given
        when(sampleLogRepository.findById("sample-1")).thenReturn(Optional.of(testSample));

        // When
        Optional<SampleLog> result = sampleService.getSampleLogById("sample-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("sample-1");
    }

    @Test
    void getSampleLogsByBatch_shouldReturnListOfSamples() {
        // Given
        List<SampleLog> samples = Arrays.asList(testSample);
        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(samples);

        // When
        List<SampleLog> result = sampleService.getSampleLogsByBatch("batch-1");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBatchId()).isEqualTo("batch-1");
    }

    @Test
    void searchSampleLogs_shouldFilterByBatchId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<SampleLog> samples = Arrays.asList(testSample);
        Page<SampleLog> page = new PageImpl<>(samples, pageable, samples.size());

        when(sampleLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // When
        Page<SampleLog> result = sampleService.searchSampleLogs("batch-1", null, null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchSampleLogs_shouldFilterByPondId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<SampleLog> samples = Arrays.asList(testSample);
        Page<SampleLog> page = new PageImpl<>(samples, pageable, samples.size());

        // Use lenient stubbing to avoid unnecessary stubbing exception
        lenient().when(batchRepository.findDistinctByFishSeedsPondId("pond-1"))
                .thenReturn(Arrays.asList(testBatch));
        when(sampleLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // When
        Page<SampleLog> result = sampleService.searchSampleLogs(null, "pond-1", null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchSampleLogs_shouldReturnEmpty_whenNoBatchesForPond() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SampleLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        // Use lenient stubbing to avoid unnecessary stubbing exception
        lenient().when(batchRepository.findDistinctByFishSeedsPondId("pond-1"))
                .thenReturn(Collections.emptyList());
        when(sampleLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        // When
        Page<SampleLog> result = sampleService.searchSampleLogs(null, "pond-1", null, null, null, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    // ==================== Update Tests ====================

    @Test
    void updateSampleLog_shouldUpdateFields() {
        // Given
        SampleLog updates = SampleLog.builder()
                .averageWeight(60.0)
                .sampleCount(15)
                .build();

        when(sampleLogRepository.findById("sample-1")).thenReturn(Optional.of(testSample));
        when(sampleLogRepository.save(any(SampleLog.class))).thenReturn(testSample);

        // When
        SampleLog result = sampleService.updateSampleLog("sample-1", updates);

        // Then
        verify(sampleLogRepository).save(argThat(sample ->
                sample.getAverageWeight() == 60.0 && sample.getSampleCount() == 15
        ));
    }

    @Test
    void updateSampleLog_shouldThrowException_whenNotFound() {
        // Given
        when(sampleLogRepository.findById("sample-1")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> sampleService.updateSampleLog("sample-1", testSample))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sample log not found");
    }

    // ==================== Delete Tests ====================

    @Test
    void deleteSampleLog_shouldDeleteSuccessfully() {
        // Given
        when(sampleLogRepository.existsById("sample-1")).thenReturn(true);

        // When
        sampleService.deleteSampleLog("sample-1");

        // Then
        verify(sampleLogRepository).deleteById("sample-1");
    }

    @Test
    void deleteSampleLog_shouldThrowException_whenNotFound() {
        // Given
        when(sampleLogRepository.existsById("sample-1")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> sampleService.deleteSampleLog("sample-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sample log not found");
    }

    // ==================== Analytics Tests ====================

    @Test
    void getWeightTrendForBatch_shouldReturnSortedTrend() {
        // Given
        SampleLog sample1 = SampleLog.builder()
                .date(LocalDate.now().minusDays(2))
                .averageWeight(40.0)
                .sampleCount(10)
                .build();
        SampleLog sample2 = SampleLog.builder()
                .date(LocalDate.now())
                .averageWeight(50.0)
                .sampleCount(10)
                .build();

        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(Arrays.asList(sample2, sample1));

        // When
        List<Map<String, Object>> trend = sampleService.getWeightTrendForBatch("batch-1");

        // Then
        assertThat(trend).hasSize(2);
        assertThat((Double) trend.get(0).get("averageWeight")).isEqualTo(40.0);
        assertThat((Double) trend.get(1).get("averageWeight")).isEqualTo(50.0);
    }

    @Test
    void getGrowthRateForBatch_shouldCalculateCorrectly() {
        // Given
        SampleLog sample1 = SampleLog.builder()
                .date(LocalDate.now().minusDays(10))
                .averageWeight(40.0)
                .build();
        SampleLog sample2 = SampleLog.builder()
                .date(LocalDate.now())
                .averageWeight(50.0)
                .build();

        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(Arrays.asList(sample1, sample2));

        // When
        Double growthRate = sampleService.getGrowthRateForBatch("batch-1");

        // Then
        assertThat(growthRate).isEqualTo(1.0); // (50 - 40) / 10 = 1.0 g/day
    }

    @Test
    void getGrowthRateForBatch_shouldReturnZero_whenLessThanTwoSamples() {
        // Given
        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(Arrays.asList(testSample));

        // When
        Double growthRate = sampleService.getGrowthRateForBatch("batch-1");

        // Then
        assertThat(growthRate).isEqualTo(0.0);
    }

    @Test
    void getLatestSampleForBatch_shouldReturnMostRecent() {
        // Given
        SampleLog older = SampleLog.builder()
                .date(LocalDate.now().minusDays(5))
                .averageWeight(40.0)
                .build();
        SampleLog newer = SampleLog.builder()
                .date(LocalDate.now())
                .averageWeight(50.0)
                .build();

        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(Arrays.asList(older, newer));

        // When
        Optional<SampleLog> latest = sampleService.getLatestSampleForBatch("batch-1");

        // Then
        assertThat(latest).isPresent();
        assertThat(latest.get().getAverageWeight()).isEqualTo(50.0);
    }

    @Test
    void getAverageSurvivalForBatch_shouldCalculateAverage() {
        // Given
        SampleLog sample1 = SampleLog.builder().survivalEstimate(80.0).build();
        SampleLog sample2 = SampleLog.builder().survivalEstimate(90.0).build();

        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(Arrays.asList(sample1, sample2));

        // When
        Double avgSurvival = sampleService.getAverageSurvivalForBatch("batch-1");

        // Then
        assertThat(avgSurvival).isEqualTo(85.0);
    }

    @Test
    void getSampleLogsByDateRange_shouldFilterCorrectly() {
        // Given
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();

        SampleLog inRange = SampleLog.builder().date(LocalDate.now().minusDays(2)).build();
        SampleLog outOfRange = SampleLog.builder().date(LocalDate.now().minusDays(10)).build();

        when(sampleLogRepository.findAll()).thenReturn(Arrays.asList(inRange, outOfRange));

        // When
        List<SampleLog> result = sampleService.getSampleLogsByDateRange(start, end);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getEstimatedBiomassForBatch_shouldCalculateCorrectly() {
        // Given
        SampleLog latestSample = SampleLog.builder()
                .averageWeight(50.0) // 50 grams
                .date(LocalDate.now())
                .build();

        FishStockEvent event1 = FishStockEvent.builder().quantityDelta(1000).build();
        FishStockEvent event2 = FishStockEvent.builder().quantityDelta(-200).build();

        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(Arrays.asList(latestSample));
        when(fishStockEventRepository.findByBatchId("batch-1")).thenReturn(Arrays.asList(event1, event2));

        // When
        Double biomass = sampleService.getEstimatedBiomassForBatch("batch-1");

        // Then
        // Current stock = 1000 - 200 = 800 fish
        // Biomass = 800 × 50g / 1000 = 40 kg
        assertThat(biomass).isEqualTo(40.0);
    }

    @Test
    void getEstimatedBiomassForBatch_shouldReturnZero_whenNoSamples() {
        // Given
        when(sampleLogRepository.findByBatchId("batch-1")).thenReturn(Collections.emptyList());

        // When
        Double biomass = sampleService.getEstimatedBiomassForBatch("batch-1");

        // Then
        assertThat(biomass).isEqualTo(0.0);
    }
}
