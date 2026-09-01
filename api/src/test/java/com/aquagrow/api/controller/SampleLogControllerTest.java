package com.aquagrow.api.controller;

import com.aquagrow.api.model.SampleLog;
import com.aquagrow.api.service.SampleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SampleLogController.class)
class SampleLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SampleService sampleService;

    @Autowired
    private ObjectMapper objectMapper;

    private SampleLog testSample;

    @BeforeEach
    void setUp() {
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

    // ==================== CRUD Tests ====================

    @Test
    @WithMockUser
    void createSampleLog_shouldReturnCreatedSample() throws Exception {
        // Given
        when(sampleService.createSampleLog(any(SampleLog.class))).thenReturn(testSample);

        // When/Then
        mockMvc.perform(post("/api/sample-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSample)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sample-1"))
                .andExpect(jsonPath("$.batchId").value("batch-1"))
                .andExpect(jsonPath("$.averageWeight").value(50.0));

        verify(sampleService).createSampleLog(any(SampleLog.class));
    }

    @Test
    @WithMockUser
    void getSampleLogById_shouldReturnSample() throws Exception {
        // Given
        when(sampleService.getSampleLogById("sample-1")).thenReturn(Optional.of(testSample));

        // When/Then
        mockMvc.perform(get("/api/sample-logs/sample-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sample-1"))
                .andExpect(jsonPath("$.averageWeight").value(50.0));
    }

    @Test
    @WithMockUser
    void getSampleLogsByBatch_shouldReturnList() throws Exception {
        // Given
        when(sampleService.getSampleLogsByBatch("batch-1"))
                .thenReturn(Arrays.asList(testSample));

        // When/Then
        mockMvc.perform(get("/api/sample-logs/batch/batch-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].batchId").value("batch-1"));
    }

    @Test
    @WithMockUser
    void updateSampleLog_shouldReturnUpdated() throws Exception {
        // Given
        SampleLog updated = SampleLog.builder()
                .id("sample-1")
                .averageWeight(60.0)
                .build();

        when(sampleService.updateSampleLog(eq("sample-1"), any(SampleLog.class)))
                .thenReturn(updated);

        // When/Then
        mockMvc.perform(put("/api/sample-logs/sample-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageWeight").value(60.0));
    }

    @Test
    @WithMockUser
    void deleteSampleLog_shouldReturn200() throws Exception {
        // Given
        doNothing().when(sampleService).deleteSampleLog("sample-1");

        // When/Then
        mockMvc.perform(delete("/api/sample-logs/sample-1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(sampleService).deleteSampleLog("sample-1");
    }

    // ==================== Search Tests ====================

    @Test
    @WithMockUser
    void searchSampleLogs_shouldReturnPagedResults() throws Exception {
        // Given
        Page<SampleLog> page = new PageImpl<>(Arrays.asList(testSample), PageRequest.of(0, 10), 1);
        when(sampleService.searchSampleLogs(any(), any(), any(), any(), any(), any())).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/sample-logs")
                        .param("batchId", "batch-1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].batchId").value("batch-1"));
    }

    @Test
    @WithMockUser
    void searchSampleLogs_withPondFilter_shouldReturnResults() throws Exception {
        // Given
        Page<SampleLog> page = new PageImpl<>(Arrays.asList(testSample), PageRequest.of(0, 10), 1);
        when(sampleService.searchSampleLogs(any(), eq("pond-1"), any(), any(), any(), any())).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/sample-logs")
                        .param("pondId", "pond-1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @WithMockUser
    void searchSampleLogs_withFishTypeFilter_shouldReturnResults() throws Exception {
        // Given
        Page<SampleLog> page = new PageImpl<>(Arrays.asList(testSample), PageRequest.of(0, 10), 1);
        when(sampleService.searchSampleLogs(any(), any(), eq("fish-1"), any(), any(), any())).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/sample-logs")
                        .param("fishTypeId", "fish-1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ==================== Analytics Tests ====================

    @Test
    @WithMockUser
    void getWeightTrend_shouldReturnTrendData() throws Exception {
        // Given
        Map<String, Object> point1 = new HashMap<>();
        point1.put("date", LocalDate.now().minusDays(2).toString());
        point1.put("averageWeight", 40.0);

        Map<String, Object> point2 = new HashMap<>();
        point2.put("date", LocalDate.now().toString());
        point2.put("averageWeight", 50.0);

        when(sampleService.getWeightTrendForBatch("batch-1"))
                .thenReturn(Arrays.asList(point1, point2));

        // When/Then
        mockMvc.perform(get("/api/sample-logs/batch/batch-1/weight-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].averageWeight").value(40.0))
                .andExpect(jsonPath("$[1].averageWeight").value(50.0));
    }

    @Test
    @WithMockUser
    void getGrowthRate_shouldReturnRate() throws Exception {
        // Given
        when(sampleService.getGrowthRateForBatch("batch-1")).thenReturn(1.5);

        // When/Then
        mockMvc.perform(get("/api/sample-logs/batch/batch-1/growth-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1.5));
    }

    @Test
    @WithMockUser
    void getLatestSample_shouldReturnLatest() throws Exception {
        // Given
        when(sampleService.getLatestSampleForBatch("batch-1"))
                .thenReturn(Optional.of(testSample));

        // When/Then
        mockMvc.perform(get("/api/sample-logs/batch/batch-1/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sample-1"))
                .andExpect(jsonPath("$.averageWeight").value(50.0));
    }

    @Test
    @WithMockUser
    void getAverageSurvival_shouldReturnAverage() throws Exception {
        // Given
        when(sampleService.getAverageSurvivalForBatch("batch-1")).thenReturn(85.5);

        // When/Then
        mockMvc.perform(get("/api/sample-logs/batch/batch-1/survival"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(85.5));
    }

    @Test
    @WithMockUser
    void getEstimatedBiomass_shouldReturnBiomass() throws Exception {
        // Given
        when(sampleService.getEstimatedBiomassForBatch("batch-1")).thenReturn(125.5);

        // When/Then
        mockMvc.perform(get("/api/sample-logs/batch/batch-1/biomass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(125.5));
    }

    @Test
    @WithMockUser
    void getSampleLogsByDateRange_shouldReturnFilteredResults() throws Exception {
        // Given
        when(sampleService.getSampleLogsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testSample));

        // When/Then
        mockMvc.perform(get("/api/sample-logs/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("sample-1"));
    }
}
