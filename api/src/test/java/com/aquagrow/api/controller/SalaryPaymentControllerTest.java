package com.aquagrow.api.controller;

import com.aquagrow.api.model.SalaryPayment;
import com.aquagrow.api.service.SalaryPaymentService;
import com.aquagrow.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SalaryPaymentController.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/aquagrow",
    "cors.allowed-origins=http://localhost:3000"
})
@DisplayName("SalaryPaymentController Tests")
class SalaryPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SalaryPaymentService salaryPaymentService;

    private SalaryPayment testPayment1;
    private SalaryPayment testPayment2;
    private List<SalaryPayment> testPayments;

    @BeforeEach
    void setUp() {
        testPayment1 = SalaryPayment.builder()
                .id("pay-001")
                .employeeId("emp-001")
                .date(LocalDate.of(2024, 1, 31))
                .baseSalary(5000.0)
                .advanceDeduction(500.0)
                .bonus(200.0)
                .netAmount(4700.0)
                .month("2024-01")
                .tenantId("test-tenant")
                .build();

        testPayment2 = SalaryPayment.builder()
                .id("pay-002")
                .employeeId("emp-002")
                .date(LocalDate.of(2024, 1, 31))
                .baseSalary(6000.0)
                .advanceDeduction(0.0)
                .bonus(500.0)
                .netAmount(6500.0)
                .month("2024-01")
                .tenantId("test-tenant")
                .build();

        testPayments = Arrays.asList(testPayment1, testPayment2);

        // Mock TenantContext
        TenantContext.setTenantId("test-tenant");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/salary-payments - Should return all salary payments")
    void getAllPayments_shouldReturnAllPayments() throws Exception {
        when(salaryPaymentService.getAllPayments()).thenReturn(testPayments);

        mockMvc.perform(get("/api/salary-payments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("pay-001")))
                .andExpect(jsonPath("$[0].employeeId", is("emp-001")))
                .andExpect(jsonPath("$[0].netAmount", is(4700.0)))
                .andExpect(jsonPath("$[1].id", is("pay-002")))
                .andExpect(jsonPath("$[1].employeeId", is("emp-002")));

        verify(salaryPaymentService, times(1)).getAllPayments();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/salary-payments/{id} - Should return salary payment by ID")
    void getPaymentById_shouldReturnPayment_whenExists() throws Exception {
        when(salaryPaymentService.getPaymentById("pay-001")).thenReturn(testPayment1);

        mockMvc.perform(get("/api/salary-payments/pay-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("pay-001")))
                .andExpect(jsonPath("$.employeeId", is("emp-001")))
                .andExpect(jsonPath("$.baseSalary", is(5000.0)))
                .andExpect(jsonPath("$.advanceDeduction", is(500.0)))
                .andExpect(jsonPath("$.bonus", is(200.0)))
                .andExpect(jsonPath("$.netAmount", is(4700.0)))
                .andExpect(jsonPath("$.month", is("2024-01")));

        verify(salaryPaymentService, times(1)).getPaymentById("pay-001");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/salary-payments/employee/{employeeId} - Should return employee salary history")
    void getEmployeeSalaryHistory_shouldReturnHistory() throws Exception {
        when(salaryPaymentService.getEmployeeSalaryHistory("emp-001"))
                .thenReturn(Arrays.asList(testPayment1));

        mockMvc.perform(get("/api/salary-payments/employee/emp-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId", is("emp-001")));

        verify(salaryPaymentService, times(1)).getEmployeeSalaryHistory("emp-001");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/salary-payments/month/{month} - Should return payments for month")
    void getPaymentsForMonth_shouldReturnPaymentsForMonth() throws Exception {
        when(salaryPaymentService.getPaymentsForMonth("2024-01")).thenReturn(testPayments);

        mockMvc.perform(get("/api/salary-payments/month/2024-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].month", is("2024-01")))
                .andExpect(jsonPath("$[1].month", is("2024-01")));

        verify(salaryPaymentService, times(1)).getPaymentsForMonth("2024-01");
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/salary-payments - Should create new salary payment")
    void createPayment_shouldCreatePayment_whenValid() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("employeeId", "emp-001");
        requestBody.put("date", "2024-01-31");
        requestBody.put("baseSalary", 5000.0);
        requestBody.put("advanceDeduction", 500.0);
        requestBody.put("bonus", 200.0);

        when(salaryPaymentService.createSalaryPayment(
                eq("emp-001"),
                any(LocalDate.class),
                eq(5000.0),
                eq(500.0),
                eq(200.0)
        )).thenReturn(testPayment1);

        mockMvc.perform(post("/api/salary-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("pay-001")))
                .andExpect(jsonPath("$.employeeId", is("emp-001")))
                .andExpect(jsonPath("$.netAmount", is(4700.0)));

        verify(salaryPaymentService, times(1)).createSalaryPayment(
                eq("emp-001"),
                any(LocalDate.class),
                eq(5000.0),
                eq(500.0),
                eq(200.0)
        );
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/salary-payments - Should handle null bonus and advance")
    void createPayment_shouldHandleNullValues() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("employeeId", "emp-001");
        requestBody.put("date", "2024-01-31");
        requestBody.put("baseSalary", 5000.0);

        SalaryPayment paymentWithDefaults = SalaryPayment.builder()
                .id("pay-001")
                .employeeId("emp-001")
                .date(LocalDate.of(2024, 1, 31))
                .baseSalary(5000.0)
                .advanceDeduction(0.0)
                .bonus(0.0)
                .netAmount(5000.0)
                .month("2024-01")
                .tenantId("test-tenant")
                .build();

        when(salaryPaymentService.createSalaryPayment(
                eq("emp-001"),
                any(LocalDate.class),
                eq(5000.0),
                isNull(),
                isNull()
        )).thenReturn(paymentWithDefaults);

        mockMvc.perform(post("/api/salary-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netAmount", is(5000.0)));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/salary-payments/{id} - Should delete salary payment")
    void deletePayment_shouldDeletePayment() throws Exception {
        doNothing().when(salaryPaymentService).deletePayment("pay-001");

        mockMvc.perform(delete("/api/salary-payments/pay-001")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(salaryPaymentService, times(1)).deletePayment("pay-001");
    }

    @Test
    @DisplayName("GET /api/salary-payments - Should return 401 when not authenticated")
    void getAllPayments_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/salary-payments"))
                .andExpect(status().isUnauthorized());
    }
}
