package com.aquagrow.api.controller;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishSeed;
import com.aquagrow.api.service.BatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for BatchController.
 * Tests REST endpoints with mocked BatchService.
 */
@WebMvcTest(BatchController.class)
@DisplayName("BatchController Tests")
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchService batchService;

    private static final String TEST_BATCH_ID = "batch-123";
    private static final String TEST_POND_ID = "pond-456";

    // ========== GET ALL BATCHES TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("GET /api/batches - should return all batches")
    void getAllBatches_shouldReturnAllBatches() throws Exception {
        // Given
        List<Batch> batches = List.of(
                Batch.builder().id("batch-1").name("Batch 1").build(),
                Batch.builder().id("batch-2").name("Batch 2").build()
        );
        when(batchService.getAllBatches(null, null, null)).thenReturn(batches);

        // When / Then
        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Batch 1")))
                .andExpect(jsonPath("$[1].name", is("Batch 2")));

        verify(batchService).getAllBatches(null, null, null);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/batches?name=test - should filter by name")
    void getAllBatches_shouldFilterByName() throws Exception {
        // Given
        List<Batch> batches = List.of(
                Batch.builder().id("batch-1").name("Test Batch").build()
        );
        when(batchService.getAllBatches("test", null, null)).thenReturn(batches);

        // When / Then
        mockMvc.perform(get("/api/batches")
                        .param("name", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Batch")));

        verify(batchService).getAllBatches("test", null, null);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/batches?pondId=pond-1 - should filter by pond")
    void getAllBatches_shouldFilterByPond() throws Exception {
        // Given
        when(batchService.getAllBatches(null, TEST_POND_ID, null)).thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/batches")
                        .param("pondId", TEST_POND_ID))
                .andExpect(status().isOk());

        verify(batchService).getAllBatches(null, TEST_POND_ID, null);
    }

    // ========== CREATE BATCH TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("POST /api/batches - should create new batch")
    void createBatch_shouldCreateNewBatch() throws Exception {
        // Given
        Batch inputBatch = Batch.builder()
                .name("New Batch")
                .stockingDate(LocalDate.of(2024, 1, 1))
                .build();

        Batch createdBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .name("New Batch")
                .stockingDate(LocalDate.of(2024, 1, 1))
                .build();

        when(batchService.createBatch(any(Batch.class))).thenReturn(createdBatch);

        // When / Then
        mockMvc.perform(post("/api/batches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputBatch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(TEST_BATCH_ID)))
                .andExpect(jsonPath("$.name", is("New Batch")));

        verify(batchService).createBatch(any(Batch.class));
    }

    // ========== GET BATCH BY ID TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("GET /api/batches/{id} - should return batch when found")
    void getBatchById_shouldReturnBatchWhenFound() throws Exception {
        // Given
        Batch batch = Batch.builder()
                .id(TEST_BATCH_ID)
                .name("Test Batch")
                .build();
        when(batchService.getBatchById(TEST_BATCH_ID)).thenReturn(Optional.of(batch));

        // When / Then
        mockMvc.perform(get("/api/batches/{id}", TEST_BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(TEST_BATCH_ID)))
                .andExpect(jsonPath("$.name", is("Test Batch")));

        verify(batchService).getBatchById(TEST_BATCH_ID);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/batches/{id} - should return 404 when not found")
    void getBatchById_shouldReturn404WhenNotFound() throws Exception {
        // Given
        when(batchService.getBatchById(TEST_BATCH_ID)).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/batches/{id}", TEST_BATCH_ID))
                .andExpect(status().isNotFound());

        verify(batchService).getBatchById(TEST_BATCH_ID);
    }

    // ========== GET BATCHES BY POND TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("GET /api/batches/pond/{pondId} - should return batches for pond")
    void getBatchesByPond_shouldReturnBatchesForPond() throws Exception {
        // Given
        List<Batch> batches = List.of(
                Batch.builder().id("batch-1").name("Batch 1").build(),
                Batch.builder().id("batch-2").name("Batch 2").build()
        );
        when(batchService.getBatchesByPond(TEST_POND_ID)).thenReturn(batches);

        // When / Then
        mockMvc.perform(get("/api/batches/pond/{pondId}", TEST_POND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(batchService).getBatchesByPond(TEST_POND_ID);
    }

    // ========== UPDATE BATCH TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("PUT /api/batches/{id} - should update batch")
    void updateBatch_shouldUpdateBatch() throws Exception {
        // Given
        Batch updateDetails = Batch.builder()
                .name("Updated Batch")
                .stockingDate(LocalDate.of(2024, 2, 1))
                .build();

        Batch updatedBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .name("Updated Batch")
                .stockingDate(LocalDate.of(2024, 2, 1))
                .build();

        when(batchService.updateBatch(eq(TEST_BATCH_ID), any(Batch.class)))
                .thenReturn(Optional.of(updatedBatch));

        // When / Then
        mockMvc.perform(put("/api/batches/{id}", TEST_BATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Batch")));

        verify(batchService).updateBatch(eq(TEST_BATCH_ID), any(Batch.class));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/batches/{id} - should return 404 when batch not found")
    void updateBatch_shouldReturn404WhenNotFound() throws Exception {
        // Given
        Batch updateDetails = Batch.builder().name("Updated").build();
        when(batchService.updateBatch(eq(TEST_BATCH_ID), any(Batch.class)))
                .thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(put("/api/batches/{id}", TEST_BATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isNotFound());
    }

    // ========== ADD FISH SEED TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("POST /api/batches/{id}/seeds - should add fish seed")
    void addFishSeed_shouldAddFishSeed() throws Exception {
        // Given
        FishSeed newSeed = FishSeed.builder()
                .pondId(TEST_POND_ID)
                .fishTypeId("fishtype-1")
                .quantity(1000)
                .build();

        Batch updatedBatch = Batch.builder()
                .id(TEST_BATCH_ID)
                .fishSeeds(new ArrayList<>(List.of(newSeed)))
                .build();

        when(batchService.addFishSeed(eq(TEST_BATCH_ID), any(FishSeed.class)))
                .thenReturn(Optional.of(updatedBatch));

        // When / Then
        mockMvc.perform(post("/api/batches/{id}/seeds", TEST_BATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(TEST_BATCH_ID)))
                .andExpect(jsonPath("$.fishSeeds", hasSize(1)));

        verify(batchService).addFishSeed(eq(TEST_BATCH_ID), any(FishSeed.class));
    }

    // ========== UPDATE STATUS TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/batches/{id}/seeds/{seedId}/status - should update seed status")
    void updateSeedStatus_shouldUpdateSeedStatus() throws Exception {
        // Given
        Batch updatedBatch = Batch.builder().id(TEST_BATCH_ID).build();
        when(batchService.updateSeedStatus(TEST_BATCH_ID, "seed-1", "Harvested"))
                .thenReturn(Optional.of(updatedBatch));

        // When / Then
        mockMvc.perform(patch("/api/batches/{id}/seeds/{seedId}/status", TEST_BATCH_ID, "seed-1")
                        .with(csrf())
                        .param("status", "Harvested"))
                .andExpect(status().isOk());

        verify(batchService).updateSeedStatus(TEST_BATCH_ID, "seed-1", "Harvested");
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/batches/{id}/status - should update batch status")
    void updateBatchStatus_shouldUpdateBatchStatus() throws Exception {
        // Given
        Batch updatedBatch = Batch.builder().id(TEST_BATCH_ID).build();
        when(batchService.updateBatchStatus(TEST_BATCH_ID, "Completed"))
                .thenReturn(Optional.of(updatedBatch));

        // When / Then
        mockMvc.perform(patch("/api/batches/{id}/status", TEST_BATCH_ID)
                        .with(csrf())
                        .param("status", "Completed"))
                .andExpect(status().isOk());

        verify(batchService).updateBatchStatus(TEST_BATCH_ID, "Completed");
    }

    // ========== MOVE FISH SEED TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("POST /api/batches/{id}/seeds/{seedId}/move - should move fish seed")
    void moveFishSeed_shouldMoveFishSeed() throws Exception {
        // Given
        Batch updatedBatch = Batch.builder().id(TEST_BATCH_ID).build();
        when(batchService.moveFishSeed(TEST_BATCH_ID, "seed-1", "pond-B", 500))
                .thenReturn(Optional.of(updatedBatch));

        // When / Then
        mockMvc.perform(post("/api/batches/{id}/seeds/{seedId}/move", TEST_BATCH_ID, "seed-1")
                        .with(csrf())
                        .param("targetPondId", "pond-B")
                        .param("quantity", "500"))
                .andExpect(status().isOk());

        verify(batchService).moveFishSeed(TEST_BATCH_ID, "seed-1", "pond-B", 500);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/batches/{id}/seeds/{seedId}/move - should move full quantity when quantity not specified")
    void moveFishSeed_shouldMoveFullQuantityWhenNotSpecified() throws Exception {
        // Given
        Batch updatedBatch = Batch.builder().id(TEST_BATCH_ID).build();
        when(batchService.moveFishSeed(TEST_BATCH_ID, "seed-1", "pond-B", null))
                .thenReturn(Optional.of(updatedBatch));

        // When / Then
        mockMvc.perform(post("/api/batches/{id}/seeds/{seedId}/move", TEST_BATCH_ID, "seed-1")
                        .with(csrf())
                        .param("targetPondId", "pond-B"))
                .andExpect(status().isOk());

        verify(batchService).moveFishSeed(TEST_BATCH_ID, "seed-1", "pond-B", null);
    }

    // ========== IMPORT CSV TESTS ==========

    @Test
    @WithMockUser
    @DisplayName("POST /api/batches/import - should import batches from CSV")
    void importCsv_shouldImportBatchesFromCsv() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "batches.csv",
                "text/csv",
                "batchName,pondName,type,quantity,sizeInches,sizeGrams,date\nBatch1,Pond1,Tilapia,1000,2.5,10.0,2024-01-01".getBytes()
        );

        List<Batch> importedBatches = List.of(
                Batch.builder().id("batch-1").name("Batch1").build()
        );

        when(batchService.importFromCsv(any())).thenReturn(importedBatches);

        // When / Then
        mockMvc.perform(multipart("/api/batches/import")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Batch1")));

        verify(batchService).importFromCsv(any());
    }

    // ========== SECURITY TESTS ==========

    @Test
    @DisplayName("GET /api/batches - should return 401 without authentication")
    void getAllBatches_shouldReturn401WithoutAuth() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isUnauthorized());

        verify(batchService, never()).getAllBatches(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/batches - should return 403 without CSRF token")
    void createBatch_shouldReturn403WithoutCsrf() throws Exception {
        // Given
        Batch batch = Batch.builder().name("Test").build();

        // When / Then
        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isForbidden());

        verify(batchService, never()).createBatch(any());
    }
}
