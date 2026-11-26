package com.aquagrow.api.controller;

import com.aquagrow.api.model.FishStockEvent;
import com.aquagrow.api.model.FishStockEventType;
import com.aquagrow.api.service.FishStockEventService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FishStockEventController.class)
class FishStockEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FishStockEventService fishStockEventService;

    private static final String BATCH_ID = "batch-1";
    private static final String POND_ID = "pond-1";
    private static final String FISH_TYPE_ID = "fish-type-1";

    @Test
    @WithMockUser
    void getAllEvents_shouldReturnAllStockEvents() throws Exception {
        // Given
        FishStockEvent event1 = createTestEvent("event-1", FishStockEventType.SEED, 1000);
        FishStockEvent event2 = createTestEvent("event-2", FishStockEventType.HARVEST, -200);
        List<FishStockEvent> events = Arrays.asList(event1, event2);

        when(fishStockEventService.getAllStockEvents()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("event-1")))
                .andExpect(jsonPath("$[1].id", is("event-2")));

        verify(fishStockEventService).getAllStockEvents();
    }

    @Test
    @WithMockUser
    void getAllEvents_shouldReturnEmpty_whenNoEvents() throws Exception {
        // Given
        when(fishStockEventService.getAllStockEvents()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllEvents_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fish-stock-events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void createEvent_shouldCreateStockEvent() throws Exception {
        // Given
        FishStockEvent newEvent = FishStockEvent.builder()
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .type(FishStockEventType.SEED)
                .fishTypeId(FISH_TYPE_ID)
                .quantityDelta(1000)
                .avgWeightGram(5.0)
                .build();

        FishStockEvent savedEvent = createTestEvent("event-1", FishStockEventType.SEED, 1000);

        when(fishStockEventService.createStockEvent(any(FishStockEvent.class))).thenReturn(savedEvent);

        // When & Then
        mockMvc.perform(post("/api/fish-stock-events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("event-1")))
                .andExpect(jsonPath("$.type", is("SEED")))
                .andExpect(jsonPath("$.quantityDelta", is(1000)));

        verify(fishStockEventService).createStockEvent(any(FishStockEvent.class));
    }

    @Test
    void createEvent_shouldRequireAuthentication() throws Exception {
        // Given
        FishStockEvent newEvent = createTestEvent(null, FishStockEventType.SEED, 1000);

        // When & Then
        mockMvc.perform(post("/api/fish-stock-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getEventsByBatch_shouldReturnEventsForBatch() throws Exception {
        // Given
        FishStockEvent event1 = createTestEvent("event-1", FishStockEventType.SEED, 1000);
        FishStockEvent event2 = createTestEvent("event-2", FishStockEventType.HARVEST, -200);
        List<FishStockEvent> events = Arrays.asList(event1, event2);

        when(fishStockEventService.getStockEventsByBatch(BATCH_ID)).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].batchId", is(BATCH_ID)))
                .andExpect(jsonPath("$[1].batchId", is(BATCH_ID)));

        verify(fishStockEventService).getStockEventsByBatch(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getEventsByBatch_shouldReturnEmpty_whenNoBatchFound() throws Exception {
        // Given
        when(fishStockEventService.getStockEventsByBatch("non-existent"))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getEventsByBatch_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getEventsByPond_shouldReturnEventsForPond() throws Exception {
        // Given
        FishStockEvent event = createTestEvent("event-1", FishStockEventType.SEED, 1000);
        List<FishStockEvent> events = Collections.singletonList(event);

        when(fishStockEventService.getStockEventsByPond(POND_ID)).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/pond/{pondId}", POND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pondId", is(POND_ID)));

        verify(fishStockEventService).getStockEventsByPond(POND_ID);
    }

    @Test
    @WithMockUser
    void getEventsByPond_shouldReturnEmpty_whenNoPondFound() throws Exception {
        // Given
        when(fishStockEventService.getStockEventsByPond("non-existent"))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/pond/{pondId}", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getEventsByPond_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/pond/{pondId}", POND_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getCurrentStock_shouldReturnStockQuantity() throws Exception {
        // Given
        when(fishStockEventService.calculateCurrentStock(BATCH_ID)).thenReturn(7500);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/current-stock", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(7500)));

        verify(fishStockEventService).calculateCurrentStock(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getCurrentStock_shouldReturnZero_whenNoStock() throws Exception {
        // Given
        when(fishStockEventService.calculateCurrentStock(BATCH_ID)).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/current-stock", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(0)));
    }

    @Test
    void getCurrentStock_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/current-stock", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getPondStock_shouldReturnPondStockQuantity() throws Exception {
        // Given
        when(fishStockEventService.calculatePondStock(POND_ID)).thenReturn(15000);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/pond/{pondId}/current-stock", POND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(15000)));

        verify(fishStockEventService).calculatePondStock(POND_ID);
    }

    @Test
    void getPondStock_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/pond/{pondId}/current-stock", POND_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getStockBreakdown_shouldReturnBreakdownByEventType() throws Exception {
        // Given
        Map<FishStockEventType, Integer> breakdown = new HashMap<>();
        breakdown.put(FishStockEventType.SEED, 10000);
        breakdown.put(FishStockEventType.HARVEST, -2000);
        breakdown.put(FishStockEventType.MORTALITY, -500);

        when(fishStockEventService.calculateStockBreakdown(BATCH_ID)).thenReturn(breakdown);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/breakdown", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.SEED", is(10000)))
                .andExpect(jsonPath("$.HARVEST", is(-2000)))
                .andExpect(jsonPath("$.MORTALITY", is(-500)));

        verify(fishStockEventService).calculateStockBreakdown(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getStockBreakdown_shouldReturnEmpty_whenNoEvents() throws Exception {
        // Given
        when(fishStockEventService.calculateStockBreakdown(BATCH_ID))
                .thenReturn(Collections.emptyMap());

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/breakdown", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getStockBreakdown_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/breakdown", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getEstimatedBiomass_shouldReturnBiomassInGrams() throws Exception {
        // Given
        when(fishStockEventService.calculateEstimatedBiomass(BATCH_ID)).thenReturn(1_125_000.0);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/biomass", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(1_125_000.0)));

        verify(fishStockEventService).calculateEstimatedBiomass(BATCH_ID);
    }

    @Test
    @WithMockUser
    void getEstimatedBiomass_shouldReturnNull_whenNoWeightData() throws Exception {
        // Given
        when(fishStockEventService.calculateEstimatedBiomass(BATCH_ID)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/biomass", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void getEstimatedBiomass_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fish-stock-events/batch/{batchId}/biomass", BATCH_ID))
                .andExpect(status().isUnauthorized());
    }

    private FishStockEvent createTestEvent(String id, FishStockEventType type, int quantityDelta) {
        return FishStockEvent.builder()
                .id(id)
                .pondId(POND_ID)
                .batchId(BATCH_ID)
                .date(LocalDate.now())
                .type(type)
                .fishTypeId(FISH_TYPE_ID)
                .quantityDelta(quantityDelta)
                .avgWeightGram(100.0)
                .build();
    }
}
