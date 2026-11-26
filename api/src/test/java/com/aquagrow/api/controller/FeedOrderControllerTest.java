package com.aquagrow.api.controller;

import com.aquagrow.api.security.SubTenantAccessDeniedException;
import com.aquagrow.api.repository.FeedOrderRepository;
import com.aquagrow.api.repository.SupplierRepository;
import com.aquagrow.api.security.TenantAuthorizationService;
import com.aquagrow.api.service.FeedOrderService;
import com.aquagrow.api.service.WhatsAppMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedOrderController.class)
class FeedOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private FeedOrderService feedOrderService;
    @MockitoBean private FeedOrderRepository feedOrderRepository;
    @MockitoBean private SupplierRepository supplierRepository;
    @MockitoBean private WhatsAppMessageService whatsAppMessageService;
    @MockitoBean private TenantAuthorizationService tenantAuthorizationService;

    @Test
    @WithMockUser
    void createOrder_whenSubTenant_returns403() throws Exception {
        doThrow(new SubTenantAccessDeniedException("Only the root tenant can perform this action"))
                .when(tenantAuthorizationService).requireRootTenant();

        mockMvc.perform(post("/api/feed-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedType\":\"Pellet A\",\"quantity\":100,\"supplierId\":\"sup-1\"}"))
                .andExpect(status().isForbidden());
    }
}
