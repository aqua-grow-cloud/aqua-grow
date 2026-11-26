package com.aquagrow.api.controller;

import com.aquagrow.api.model.MonitoringLog;
import com.aquagrow.api.service.MonitoringService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitoringLogController.class)
class MonitoringLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MonitoringService monitoringService;

    private static final String BATCH_ID = "batch-1";
    private static final String POND_ID = "pond-1";

    @Test
    @WithMockUser
    void searchMonitoringLogs_shouldReturnPagedResults() throws Exception {
        // Given
        MonitoringLog log1 = createTestLog("log-1", BATCH_ID);
        MonitoringLog log2 = createTestLog("log-2", BATCH_ID);
        Page<MonitoringLog> page = new PageImpl<>(Arrays.asList(log1, log2));

        when(monitoringService.searchMonitoringLogs(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/monitoring-logs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is("log-1")))
                .andExpect(jsonPath("$.content[1].id", is("log-2")));

        verify(monitoringService).searchMonitoringLogs(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser
    void searchMonitoringLogs_shouldFilterByBatchId() throws Exception {
        // Given
        MonitoringLog log = createTestLog("log-1", BATCH_ID);
        Page<MonitoringLog> page = new PageImpl<>(Collections.singletonList(log));

        when(monitoringService.searchMonitoringLogs(eq(BATCH_ID), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/monitoring-logs")
                        .param("batchId", BATCH_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].batchId", is(BATCH_ID)));

        verify(monitoringService).searchMonitoringLogs(eq(BATCH_ID), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser
    void searchMonitoringLogs_shouldFilterByPondId() throws Exception {
        // Given
        MonitoringLog log = createTestLog("log-1", BATCH_ID);
        Page<MonitoringLog> page = new PageImpl<>(Collections.singletonList(log));

        when(monitoringService.searchMonitoringLogs(isNull(), eq(POND_ID), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/monitoring-logs")
                        .param("pondId", POND_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(monitoringService).searchMonitoringLogs(isNull(), eq(POND_ID), any(Pageable.class));
    }

    @Test
    @WithMockUser
    void searchMonitoringLogs_shouldFilterByBothBatchAndPond() throws Exception {
        // Given
        MonitoringLog log = createTestLog("log-1", BATCH_ID);
        Page<MonitoringLog> page = new PageImpl<>(Collections.singletonList(log));

        when(monitoringService.searchMonitoringLogs(eq(BATCH_ID), eq(POND_ID), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/monitoring-logs")
                        .param("batchId", BATCH_ID)
                        .param("pondId", POND_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(monitoringService).searchMonitoringLogs(eq(BATCH_ID), eq(POND_ID), any(Pageable.class));
    }

    @Test
    @WithMockUser
    void searchMonitoringLogs_shouldReturnEmptyPage_whenNoResults() throws Exception {
        // Given
        Page<MonitoringLog> emptyPage = new PageImpl<>(Collections.emptyList());

        when(monitoringService.searchMonitoringLogs(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/monitoring-logs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void searchMonitoringLogs_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/monitoring-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void logMonitoring_shouldCreateMonitoringLog() throws Exception {
        // Given
        MonitoringLog newLog = MonitoringLog.builder()
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .doLevel(7.5)
                .ph(8.0)
                .temperature(28.0)
                .build();

        MonitoringLog savedLog = createTestLog("log-1", BATCH_ID);

        when(monitoringService.createMonitoringLog(any(MonitoringLog.class))).thenReturn(savedLog);

        // When & Then
        mockMvc.perform(post("/api/monitoring-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLog)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("log-1")))
                .andExpect(jsonPath("$.batchId", is(BATCH_ID)))
                .andExpect(jsonPath("$.doLevel", is(7.5)))
                .andExpect(jsonPath("$.ph", is(8.0)))
                .andExpect(jsonPath("$.temperature", is(28.0)));

        verify(monitoringService).createMonitoringLog(any(MonitoringLog.class));
    }

    @Test
    @WithMockUser
    void logMonitoring_shouldCreateWithAllWaterQualityParameters() throws Exception {
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

        MonitoringLog savedLog = MonitoringLog.builder()
                .id("log-1")
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .doLevel(7.5)
                .ph(8.0)
                .temperature(28.0)
                .ammonia(0.1)
                .salinity(15.0)
                .turbidity(20.0)
                .build();

        when(monitoringService.createMonitoringLog(any(MonitoringLog.class))).thenReturn(savedLog);

        // When & Then
        mockMvc.perform(post("/api/monitoring-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullLog)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ammonia", is(0.1)))
                .andExpect(jsonPath("$.salinity", is(15.0)))
                .andExpect(jsonPath("$.turbidity", is(20.0)));
    }

    @Test
    void logMonitoring_shouldRequireAuthentication() throws Exception {
        // Given
        MonitoringLog newLog = createTestLog(null, BATCH_ID);

        // When & Then
        mockMvc.perform(post("/api/monitoring-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLog)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getMonitoringLogsByBatch_shouldReturnAllLogsForBatch() throws Exception {
        // Given
        MonitoringLog log1 = createTestLog("log-1", BATCH_ID);
        MonitoringLog log2 = createTestLog("log-2", BATCH_ID);
        List<MonitoringLog> logs = Arrays.asList(log1, log2);

        when(monitoringService.getMonitoringLogsByBatch(BATCH_ID)).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/monitoring-logs/batch/{batchId}", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("log-1")))
                .andExpect(jsonPath("$[1].id", is("log-2")))
                .andExpect(jsonPath("$[0].batchId", is(BATCH_ID)))
                .andExpect(jsonPath("$[1].batchId", is(BATCH_ID)));

        verify(monitoringService).getMonitoringLogsByBatch(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getMonitoringLogsByBatch_shouldReturnEmptyList_whenNoBatchFound() throws Exception {
        // Given
        when(monitoringService.getMonitoringLogsByBatch("non-existent"))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/monitoring-logs/batch/{batchId}", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMonitoringLogsByBatch_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/monitoring-logs/batch/{batchId}", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    private MonitoringLog createTestLog(String id, String batchId) {
        return MonitoringLog.builder()
                .id(id)
                .batchId(batchId)
                .date(LocalDate.now())
                .doLevel(7.5)
                .ph(8.0)
                .temperature(28.0)
                .ammonia(0.1)
                .salinity(15.0)
                .turbidity(20.0)
                .build();
    }
}
