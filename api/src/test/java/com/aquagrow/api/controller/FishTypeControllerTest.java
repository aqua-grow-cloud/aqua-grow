package com.aquagrow.api.controller;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishType;
import com.aquagrow.api.service.FishTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FishTypeController.class)
class FishTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FishTypeService fishTypeService;

    @Autowired
    private ObjectMapper objectMapper;

    private FishType testFishType;

    @BeforeEach
    void setUp() {
        testFishType = FishType.builder()
                .id("fish-1")
                .name("Tilapia")
                .scientificName("Oreochromis niloticus")
                .tenantId("tenant-123")
                .build();
    }

    // ==================== CRUD Tests ====================

    @Test
    @WithMockUser
    void getAllFishTypes_shouldReturnAll() throws Exception {
        // Given
        when(fishTypeService.getAllFishTypes()).thenReturn(Arrays.asList(testFishType));

        // When/Then
        mockMvc.perform(get("/api/fish-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Tilapia")))
                .andExpect(jsonPath("$[0].scientificName", is("Oreochromis niloticus")));
    }

    @Test
    @WithMockUser
    void getFishTypeById_shouldReturnFishType() throws Exception {
        // Given
        when(fishTypeService.getFishTypeById("fish-1")).thenReturn(Optional.of(testFishType));

        // When/Then
        mockMvc.perform(get("/api/fish-types/fish-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("fish-1")))
                .andExpect(jsonPath("$.name", is("Tilapia")));
    }

    @Test
    @WithMockUser
    void createFishType_shouldReturnCreated() throws Exception {
        // Given
        when(fishTypeService.createFishType(any(FishType.class))).thenReturn(testFishType);

        // When/Then
        mockMvc.perform(post("/api/fish-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFishType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("fish-1")))
                .andExpect(jsonPath("$.name", is("Tilapia")));

        verify(fishTypeService).createFishType(any(FishType.class));
    }

    @Test
    @WithMockUser
    void updateFishType_shouldReturnUpdated() throws Exception {
        // Given
        FishType updated = FishType.builder()
                .id("fish-1")
                .name("Nile Tilapia")
                .build();

        when(fishTypeService.updateFishType(eq("fish-1"), any(FishType.class)))
                .thenReturn(updated);

        // When/Then
        mockMvc.perform(put("/api/fish-types/fish-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Nile Tilapia")));
    }

    @Test
    @WithMockUser
    void deleteFishType_shouldReturn200() throws Exception {
        // Given
        doNothing().when(fishTypeService).deleteFishType("fish-1");

        // When/Then
        mockMvc.perform(delete("/api/fish-types/fish-1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(fishTypeService).deleteFishType("fish-1");
    }

    // ==================== Analytics Tests ====================

    @Test
    @WithMockUser
    void searchFishTypes_shouldReturnMatches() throws Exception {
        // Given
        when(fishTypeService.searchByName("til")).thenReturn(Arrays.asList(testFishType));

        // When/Then
        mockMvc.perform(get("/api/fish-types/search")
                        .param("query", "til"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Tilapia")));
    }

    @Test
    @WithMockUser
    void isInUse_shouldReturnBoolean() throws Exception {
        // Given
        when(fishTypeService.isInUse("fish-1")).thenReturn(true);

        // When/Then
        mockMvc.perform(get("/api/fish-types/fish-1/in-use"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser
    void getBatchCount_shouldReturnCount() throws Exception {
        // Given
        when(fishTypeService.getBatchCountForFishType("fish-1")).thenReturn(5L);

        // When/Then
        mockMvc.perform(get("/api/fish-types/fish-1/batch-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser
    void getBatches_shouldReturnBatches() throws Exception {
        // Given
        Batch batch = Batch.builder().id("batch-1").name("Test Batch").build();
        when(fishTypeService.getBatchesForFishType("fish-1")).thenReturn(Arrays.asList(batch));

        // When/Then
        mockMvc.perform(get("/api/fish-types/fish-1/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("batch-1")));
    }
}
