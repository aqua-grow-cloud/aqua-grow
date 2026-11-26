package com.aquagrow.api.controller;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Pond;
import com.aquagrow.api.service.PondService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PondController.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/aquagrow",
    "cors.allowed-origins=http://localhost:3000"
})
@DisplayName("PondController Tests")
class PondControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PondService pondService;

    private static final String POND_ID = "pond-1";

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds - Should return all ponds")
    void getAllPonds_shouldReturnAllPonds() throws Exception {
        // Given
        Pond pond1 = createTestPond("1", "Pond 1", 100.0);
        Pond pond2 = createTestPond("2", "Pond 2", 150.0);
        when(pondService.getAllPonds()).thenReturn(Arrays.asList(pond1, pond2));

        // When & Then
        mockMvc.perform(get("/api/ponds"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Pond 1")))
                .andExpect(jsonPath("$[1].name", is("Pond 2")));

        verify(pondService).getAllPonds();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/ponds - Should create new pond")
    void createPond_shouldCreatePond() throws Exception {
        // Given
        Pond newPond = Pond.builder()
                .name("New Pond")
                .area(200.0)
                .status("Active")
                .build();

        Pond savedPond = createTestPond("3", "New Pond", 200.0);
        when(pondService.createPond(any(Pond.class))).thenReturn(savedPond);

        // When & Then
        mockMvc.perform(post("/api/ponds")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPond)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Pond")))
                .andExpect(jsonPath("$.area", is(200.0)));

        verify(pondService).createPond(any(Pond.class));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id} - Should return pond by id")
    void getPondById_shouldReturnPond() throws Exception {
        // Given
        Pond pond = createTestPond(POND_ID, "Test Pond", 100.0);
        when(pondService.getPondById(POND_ID)).thenReturn(pond);

        // When & Then
        mockMvc.perform(get("/api/ponds/{id}", POND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(POND_ID)))
                .andExpect(jsonPath("$.name", is("Test Pond")));

        verify(pondService).getPondById(POND_ID);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id} - Should return 404 when not found")
    void getPondById_shouldThrowException_whenNotFound() throws Exception {
        // Given
        when(pondService.getPondById("non-existent"))
                .thenThrow(new BusinessException("Pond not found"));

        // When & Then
        mockMvc.perform(get("/api/ponds/{id}", "non-existent"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/ponds/{id} - Should update existing pond")
    void updatePond_shouldUpdatePond() throws Exception {
        // Given
        Pond updates = Pond.builder()
                .name("Updated Pond")
                .area(120.0)
                .status("Maintenance")
                .build();

        Pond updated = createTestPond(POND_ID, "Updated Pond", 120.0);
        when(pondService.updatePond(eq(POND_ID), any(Pond.class))).thenReturn(updated);

        // When & Then
        mockMvc.perform(put("/api/ponds/{id}", POND_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Pond")));

        verify(pondService).updatePond(eq(POND_ID), any(Pond.class));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/ponds/{id} - Should delete pond")
    void deletePond_shouldDeletePond() throws Exception {
        // Given
        doNothing().when(pondService).deletePond(POND_ID);

        // When & Then
        mockMvc.perform(delete("/api/ponds/{id}", POND_ID)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(pondService).deletePond(POND_ID);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id}/volume - Should return water volume")
    void getWaterVolume_shouldReturnVolume() throws Exception {
        // Given
        when(pondService.calculateWaterVolume(POND_ID)).thenReturn(2000.0);

        // When & Then
        mockMvc.perform(get("/api/ponds/{id}/volume", POND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(2000.0)));

        verify(pondService).calculateWaterVolume(POND_ID);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id}/max-capacity - Should return max stocking capacity")
    void getMaxStockingCapacity_shouldReturnCapacity() throws Exception {
        // Given
        when(pondService.calculateMaxStockingCapacity(POND_ID)).thenReturn(20000);

        // When & Then
        mockMvc.perform(get("/api/ponds/{id}/max-capacity", POND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(20000)));

        verify(pondService).calculateMaxStockingCapacity(POND_ID);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id}/stocking-density - Should return current stocking density")
    void getCurrentStockingDensity_shouldReturnDensity() throws Exception {
        // Given
        when(pondService.calculateCurrentStockingDensity(POND_ID, 5000)).thenReturn(5.0);

        // When & Then
        mockMvc.perform(get("/api/ponds/{id}/stocking-density", POND_ID)
                        .param("currentStock", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(5.0)));

        verify(pondService).calculateCurrentStockingDensity(POND_ID, 5000);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id}/can-accommodate - Should return true when can accommodate")
    void canAccommodateFish_shouldReturnTrue() throws Exception {
        // Given
        when(pondService.canAccommodateFish(POND_ID, 10000, 5000)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/ponds/{id}/can-accommodate", POND_ID)
                        .param("currentStock", "10000")
                        .param("additionalFish", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));

        verify(pondService).canAccommodateFish(POND_ID, 10000, 5000);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/status/{status} - Should return ponds by status")
    void getPondsByStatus_shouldReturnFilteredPonds() throws Exception {
        // Given
        Pond pond1 = createTestPond("1", "Active Pond 1", 100.0);
        pond1.setStatus("Active");
        when(pondService.getPondsByStatus("Active")).thenReturn(Collections.singletonList(pond1));

        // When & Then
        mockMvc.perform(get("/api/ponds/status/{status}", "Active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("Active")));

        verify(pondService).getPondsByStatus("Active");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/active - Should return active ponds")
    void getActivePonds_shouldReturnActivePonds() throws Exception {
        // Given
        Pond pond = createTestPond("1", "Active Pond", 100.0);
        when(pondService.getActivePonds()).thenReturn(Collections.singletonList(pond));

        // When & Then
        mockMvc.perform(get("/api/ponds/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(pondService).getActivePonds();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/available - Should return available ponds")
    void getAvailablePonds_shouldReturnAvailablePonds() throws Exception {
        // Given
        Pond pond = createTestPond("1", "Available Pond", 100.0);
        when(pondService.getAvailablePonds()).thenReturn(Collections.singletonList(pond));

        // When & Then
        mockMvc.perform(get("/api/ponds/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(pondService).getAvailablePonds();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/total-area - Should return total pond area")
    void getTotalPondArea_shouldReturnTotalArea() throws Exception {
        // Given
        when(pondService.calculateTotalPondArea()).thenReturn(4500.0);

        // When & Then
        mockMvc.perform(get("/api/ponds/total-area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(4500.0)));

        verify(pondService).calculateTotalPondArea();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/utilization - Should return pond utilization percentage")
    void getPondUtilization_shouldReturnUtilization() throws Exception {
        // Given
        when(pondService.calculatePondUtilization()).thenReturn(66.67);

        // When & Then
        mockMvc.perform(get("/api/ponds/utilization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(66.67)));

        verify(pondService).calculatePondUtilization();
    }

    @Test
    void getAllPonds_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/ponds"))
                .andExpect(status().isUnauthorized());
    }

    private Pond createTestPond(String id, String name, Double area) {
        return Pond.builder()
                .id(id)
                .name(name)
                .area(area)
                .depth(2.0)
                .type("intensive")
                .status("Active")
                .tenantId("test-tenant")
                .build();
    }
}
