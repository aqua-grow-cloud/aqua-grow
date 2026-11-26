package com.aquagrow.api.controller;

import com.aquagrow.api.dto.BulkFeedLogRequest;
import com.aquagrow.api.dto.BulkFeedLogResponse;
import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.service.FeedService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@WebMvcTest(FeedLogController.class)
class FeedLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedService feedService;

    private static final String BATCH_ID = "batch-1";
    private static final String FEED_TYPE = "Premium Feed";

    @Test
    @WithMockUser
    void getAllFeedLogs_shouldReturnAllLogs() throws Exception {
        // Given
        FeedLog log1 = createTestLog("log-1", 50.0);
        FeedLog log2 = createTestLog("log-2", 75.0);
        List<FeedLog> logs = Arrays.asList(log1, log2);

        when(feedService.getAllFeedLogs()).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/feed-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("log-1")))
                .andExpect(jsonPath("$[1].id", is("log-2")));

        verify(feedService).getAllFeedLogs();
    }

    @Test
    @WithMockUser
    void getAllFeedLogs_shouldReturnEmpty_whenNoLogs() throws Exception {
        // Given
        when(feedService.getAllFeedLogs()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/feed-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllFeedLogs_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void logFeed_shouldCreateFeedLog() throws Exception {
        // Given
        FeedLog newLog = FeedLog.builder()
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .feedType(FEED_TYPE)
                .amount(50.0)
                .time("08:00")
                .build();

        FeedLog savedLog = createTestLog("log-1", 50.0);

        when(feedService.createFeedLog(any(FeedLog.class))).thenReturn(savedLog);

        // When & Then
        mockMvc.perform(post("/api/feed-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLog)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("log-1")))
                .andExpect(jsonPath("$.amount", is(50.0)));

        verify(feedService).createFeedLog(any(FeedLog.class));
    }

    @Test
    void logFeed_shouldRequireAuthentication() throws Exception {
        // Given
        FeedLog newLog = createTestLog(null, 50.0);

        // When & Then
        mockMvc.perform(post("/api/feed-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLog)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void logFeedBulk_shouldReturnCreatedIdsAndTotals() throws Exception {
        BulkFeedLogRequest request = new BulkFeedLogRequest(BATCH_ID, FEED_TYPE, List.of(
                new BulkFeedLogRequest.Entry(LocalDate.of(2026, 10, 1), null, 440.0),
                new BulkFeedLogRequest.Entry(LocalDate.of(2026, 10, 2), null, 440.0)));
        when(feedService.createFeedLogsBulk(any(BulkFeedLogRequest.class)))
                .thenReturn(new BulkFeedLogResponse(List.of("a", "b"), 880.0, 4120.0));

        mockMvc.perform(post("/api/feed-logs/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdIds", hasSize(2)))
                .andExpect(jsonPath("$.totalKg", is(880.0)))
                .andExpect(jsonPath("$.remainingStockKg", is(4120.0)));

        verify(feedService).createFeedLogsBulk(any(BulkFeedLogRequest.class));
    }

    @Test
    @WithMockUser
    void logFeedBulk_insufficientStock_shouldReturnBadRequest() throws Exception {
        BulkFeedLogRequest request = new BulkFeedLogRequest(BATCH_ID, FEED_TYPE, List.of(
                new BulkFeedLogRequest.Entry(LocalDate.of(2026, 10, 1), null, 440.0)));
        when(feedService.createFeedLogsBulk(any(BulkFeedLogRequest.class)))
                .thenThrow(new BusinessException("Insufficient global feed stock"));

        mockMvc.perform(post("/api/feed-logs/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logFeedBulk_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/feed-logs/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getFeedLogsByBatch_shouldReturnLogsForBatch() throws Exception {
        // Given
        FeedLog log1 = createTestLog("log-1", 50.0);
        FeedLog log2 = createTestLog("log-2", 75.0);
        List<FeedLog> logs = Arrays.asList(log1, log2);

        when(feedService.getFeedLogsByBatch(BATCH_ID)).thenReturn(logs);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].batchId", is(BATCH_ID)))
                .andExpect(jsonPath("$[1].batchId", is(BATCH_ID)));

        verify(feedService).getFeedLogsByBatch(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getFeedLogsByBatch_shouldReturnEmpty_whenNoBatchFound() throws Exception {
        // Given
        when(feedService.getFeedLogsByBatch("non-existent"))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getFeedLogsByBatch_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getTotalFeedConsumed_shouldReturnTotalAmount() throws Exception {
        // Given
        when(feedService.calculateTotalFeedConsumed(BATCH_ID)).thenReturn(250.0);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/total", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(250.0)));

        verify(feedService).calculateTotalFeedConsumed(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getTotalFeedConsumed_shouldReturnZero_whenNoLogs() throws Exception {
        // Given
        when(feedService.calculateTotalFeedConsumed(BATCH_ID)).thenReturn(0.0);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/total", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(0.0)));
    }

    @Test
    void getTotalFeedConsumed_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/total", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getFeedConsumedInDateRange_shouldReturnAmountForDateRange() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now();

        when(feedService.calculateFeedConsumedInDateRange(eq(BATCH_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(150.0);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/total-range", BATCH_ID)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(150.0)));

        verify(feedService).calculateFeedConsumedInDateRange(eq(BATCH_ID), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getFeedConsumedInDateRange_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/total-range", BATCH_ID)
                        .param("startDate", LocalDate.now().minusDays(10).toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getFeedBreakdownByType_shouldReturnBreakdownMap() throws Exception {
        // Given
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("Feed A", 150.0);
        breakdown.put("Feed B", 100.0);

        when(feedService.calculateFeedBreakdownByType(BATCH_ID)).thenReturn(breakdown);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/breakdown", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['Feed A']", is(150.0)))
                .andExpect(jsonPath("$.['Feed B']", is(100.0)));

        verify(feedService).calculateFeedBreakdownByType(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getFeedBreakdownByType_shouldReturnEmpty_whenNoLogs() throws Exception {
        // Given
        when(feedService.calculateFeedBreakdownByType(BATCH_ID))
                .thenReturn(Collections.emptyMap());

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/breakdown", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getFeedBreakdownByType_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/breakdown", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getAverageDailyFeed_shouldReturnAverageAmount() throws Exception {
        // Given
        when(feedService.calculateAverageDailyFeed(BATCH_ID)).thenReturn(75.5);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/average-daily", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(75.5)));

        verify(feedService).calculateAverageDailyFeed(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getAverageDailyFeed_shouldReturnZero_whenNoLogs() throws Exception {
        // Given
        when(feedService.calculateAverageDailyFeed(BATCH_ID)).thenReturn(0.0);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/average-daily", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(0.0)));
    }

    @Test
    void getAverageDailyFeed_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/average-daily", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void calculateFCR_shouldReturnFCRValue() throws Exception {
        // Given
        when(feedService.calculateFCR(eq(BATCH_ID), eq(150.0), eq(50.0), eq(100.0)))
                .thenReturn(3.0);

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/fcr", BATCH_ID)
                        .param("totalFeedKg", "150.0")
                        .param("initialBiomassKg", "50.0")
                        .param("currentBiomassKg", "100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(3.0)));

        verify(feedService).calculateFCR(BATCH_ID, 150.0, 50.0, 100.0);
    }

    @Test
    @WithMockUser
    void calculateFCR_shouldReturnNull_whenInvalidParameters() throws Exception {
        // Given
        when(feedService.calculateFCR(eq(BATCH_ID), eq(150.0), eq(100.0), eq(100.0)))
                .thenReturn(null); // No biomass gain

        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/fcr", BATCH_ID)
                        .param("totalFeedKg", "150.0")
                        .param("initialBiomassKg", "100.0")
                        .param("currentBiomassKg", "100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void calculateFCR_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs/batch/{batchId}/fcr", BATCH_ID)
                        .param("totalFeedKg", "150.0")
                        .param("initialBiomassKg", "50.0")
                        .param("currentBiomassKg", "100.0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getLowStockAlerts_shouldReturnLowStockItems() throws Exception {
        // Given
        FeedStock stock1 = FeedStock.builder()
                .id("stock-1")
                .feedType("Feed A")
                .currentStock(50.0)
                .threshold(100.0)
                .build();
        FeedStock stock2 = FeedStock.builder()
                .id("stock-2")
                .feedType("Feed B")
                .currentStock(20.0)
                .threshold(100.0)
                .build();

        when(feedService.getLowStockAlerts()).thenReturn(Arrays.asList(stock1, stock2));

        // When & Then
        mockMvc.perform(get("/api/feed-logs/low-stock-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].feedType", is("Feed A")))
                .andExpect(jsonPath("$[1].feedType", is("Feed B")));

        verify(feedService).getLowStockAlerts();
    }

    @Test
    @WithMockUser
    void getLowStockAlerts_shouldReturnEmpty_whenNoLowStock() throws Exception {
        // Given
        when(feedService.getLowStockAlerts()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/feed-logs/low-stock-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getLowStockAlerts_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/feed-logs/low-stock-alerts"))
                .andExpect(status().isUnauthorized());
    }

    private FeedLog createTestLog(String id, Double amount) {
        return FeedLog.builder()
                .id(id)
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .feedType(FEED_TYPE)
                .amount(amount)
                .time("08:00")
                .build();
    }
}
