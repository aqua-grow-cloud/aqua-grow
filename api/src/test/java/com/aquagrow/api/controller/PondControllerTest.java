package com.aquagrow.api.controller;

import com.aquagrow.api.model.Pond;
import com.aquagrow.api.repository.PondRepository;
import com.aquagrow.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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

    @MockBean
    private PondRepository pondRepository;

    private Pond testPond1;
    private Pond testPond2;
    private List<Pond> testPonds;

    @BeforeEach
    void setUp() {
        testPond1 = Pond.builder()
                .id("1")
                .name("Pond 1")
                .tenantId("test-tenant")
                .area(100.0)
                .depth(2.5)
                .type("Concrete")
                .status("Active")
                .build();

        testPond2 = Pond.builder()
                .id("2")
                .name("Pond 2")
                .tenantId("test-tenant")
                .area(150.0)
                .depth(3.0)
                .type("Earthen")
                .status("Inactive")
                .build();

        testPonds = Arrays.asList(testPond1, testPond2);

        // Mock TenantContext
        TenantContext.setTenantId("test-tenant");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds - Should return all ponds")
    void getAllPonds_shouldReturnAllPonds() throws Exception {
        when(pondRepository.findAll()).thenReturn(testPonds);

        mockMvc.perform(get("/api/ponds"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].name", is("Pond 1")))
                .andExpect(jsonPath("$[0].area", is(100.0)))
                .andExpect(jsonPath("$[1].id", is("2")))
                .andExpect(jsonPath("$[1].name", is("Pond 2")));

        verify(pondRepository, times(1)).findAll();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id} - Should return pond by id")
    void getPondById_shouldReturnPond_whenExists() throws Exception {
        when(pondRepository.findById("1")).thenReturn(Optional.of(testPond1));

        mockMvc.perform(get("/api/ponds/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Pond 1")))
                .andExpect(jsonPath("$.tenantId", is("test-tenant")))
                .andExpect(jsonPath("$.area", is(100.0)));

        verify(pondRepository, times(1)).findById("1");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ponds/{id} - Should return 404 when pond not found")
    void getPondById_shouldReturn404_whenNotExists() throws Exception {
        when(pondRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ponds/999"))
                .andExpect(status().isNotFound());

        verify(pondRepository, times(1)).findById("999");
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/ponds - Should create new pond")
    void createPond_shouldCreatePond() throws Exception {
        Pond newPond = Pond.builder()
                .name("New Pond")
                .area(200.0)
                .depth(2.0)
                .type("Concrete")
                .status("Active")
                .build();

        Pond savedPond = Pond.builder()
                .id("3")
                .name("New Pond")
                .tenantId("test-tenant")
                .area(200.0)
                .depth(2.0)
                .type("Concrete")
                .status("Active")
                .build();

        when(pondRepository.save(any(Pond.class))).thenReturn(savedPond);

        mockMvc.perform(post("/api/ponds")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPond)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("3")))
                .andExpect(jsonPath("$.name", is("New Pond")))
                .andExpect(jsonPath("$.tenantId", is("test-tenant")))
                .andExpect(jsonPath("$.area", is(200.0)));

        verify(pondRepository, times(1)).save(any(Pond.class));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/ponds/{id} - Should update existing pond")
    void updatePond_shouldUpdatePond_whenExists() throws Exception {
        Pond updatedDetails = Pond.builder()
                .name("Updated Pond")
                .area(120.0)
                .depth(3.0)
                .type("Earthen")
                .status("Maintenance")
                .build();

        Pond updatedPond = Pond.builder()
                .id("1")
                .name("Updated Pond")
                .tenantId("test-tenant")
                .area(120.0)
                .depth(3.0)
                .type("Earthen")
                .status("Maintenance")
                .build();

        when(pondRepository.findById("1")).thenReturn(Optional.of(testPond1));
        when(pondRepository.save(any(Pond.class))).thenReturn(updatedPond);

        mockMvc.perform(put("/api/ponds/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Updated Pond")))
                .andExpect(jsonPath("$.area", is(120.0)))
                .andExpect(jsonPath("$.status", is("Maintenance")));

        verify(pondRepository, times(1)).findById("1");
        verify(pondRepository, times(1)).save(any(Pond.class));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/ponds/{id} - Should return 404 when pond not found")
    void updatePond_shouldReturn404_whenNotExists() throws Exception {
        Pond updatedDetails = Pond.builder()
                .name("Updated Pond")
                .area(120.0)
                .build();

        when(pondRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/ponds/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isNotFound());

        verify(pondRepository, times(1)).findById("999");
        verify(pondRepository, never()).save(any(Pond.class));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/ponds/{id} - Should delete pond")
    void deletePond_shouldDeletePond_whenExists() throws Exception {
        when(pondRepository.findById("1")).thenReturn(Optional.of(testPond1));
        doNothing().when(pondRepository).delete(testPond1);

        mockMvc.perform(delete("/api/ponds/1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(pondRepository, times(1)).findById("1");
        verify(pondRepository, times(1)).delete(testPond1);
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/ponds/{id} - Should return 404 when pond not found")
    void deletePond_shouldReturn404_whenNotExists() throws Exception {
        when(pondRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/ponds/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(pondRepository, times(1)).findById("999");
        verify(pondRepository, never()).delete(any(Pond.class));
    }
}
