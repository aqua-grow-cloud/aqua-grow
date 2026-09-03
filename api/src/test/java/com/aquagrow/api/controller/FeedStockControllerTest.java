package com.aquagrow.api.controller;

import com.aquagrow.api.security.SubTenantAccessDeniedException;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantAuthorizationService;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedStockController.class)
class FeedStockControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FeedStockRepository feedStockRepository;
    @MockBean private TenantAuthorizationService tenantAuthorizationService;
    @MockBean private EntityManager entityManager;

    @BeforeEach
    void setUpTenantFilterStubs() {
        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);
        when(filter.setParameter(anyString(), any())).thenReturn(filter);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser
    void getAll_returnsStockForEffectiveTenant() throws Exception {
        TenantContext.setTenantId("root-farm");
        when(feedStockRepository.findByTenantId("root-farm")).thenReturn(List.of());

        mockMvc.perform(get("/api/feed-stock"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getLow_returnsLowStockForEffectiveTenant() throws Exception {
        TenantContext.setTenantId("root-farm");
        when(feedStockRepository.findLowStockByTenantId("root-farm")).thenReturn(List.of());

        mockMvc.perform(get("/api/feed-stock/low"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void create_whenSubTenant_returns403() throws Exception {
        doThrow(new SubTenantAccessDeniedException("Only the root tenant can perform this action"))
                .when(tenantAuthorizationService).requireRootTenant();

        mockMvc.perform(post("/api/feed-stock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedType\":\"Pellet A\",\"currentStock\":100,\"threshold\":20,\"unit\":\"kg\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void create_whenRootTenant_returns200() throws Exception {
        TenantContext.setTenantId("root-farm");
        doNothing().when(tenantAuthorizationService).requireRootTenant();
        FeedStock stock = new FeedStock();
        stock.setId("s1");
        stock.setFeedType("Pellet A");
        stock.setCurrentStock(100.0);
        stock.setThreshold(20.0);
        stock.setUnit("kg");
        when(feedStockRepository.save(any())).thenReturn(stock);

        mockMvc.perform(post("/api/feed-stock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedType\":\"Pellet A\",\"currentStock\":100,\"threshold\":20,\"unit\":\"kg\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void update_whenWrongTenant_returns404() throws Exception {
        TenantContext.setTenantId("other-tenant");
        doNothing().when(tenantAuthorizationService).requireRootTenant();
        FeedStock stock = new FeedStock();
        stock.setId("s1");
        stock.setTenantId("root-farm"); // different from context
        stock.setFeedType("Pellet A");
        when(feedStockRepository.findById("s1")).thenReturn(Optional.of(stock));

        mockMvc.perform(put("/api/feed-stock/s1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedType\":\"Pellet A\",\"currentStock\":100,\"threshold\":20,\"unit\":\"kg\"}"))
                .andExpect(status().isNotFound());
    }
}
