package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.model.*;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HRFinanceMcpServiceTest {

    @Mock FarmDataClient farmData;
    @Mock ResolverService resolver;
    private HRFinanceMcpService service;

    @BeforeEach
    void setUp() {
        service = new HRFinanceMcpService(farmData, resolver);
        TenantContext.setTenantId("t1");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static Employee employee(String id, String status) {
        return Employee.builder().id(id).name("Name " + id).status(status).salary(20000.0).build();
    }

    // ---- employees and salaries: main farm account only ----

    @Test
    void employees_allAndOnlyActive_forTheMainFarmAccount() {
        Employee active = employee("e-1", "Active");
        Employee inactive = employee("e-2", "Inactive");
        when(farmData.employees()).thenReturn(List.of(active, inactive));

        assertThat(service.listEmployees()).containsExactly(active, inactive);
        assertThat(service.listActiveEmployees()).containsExactly(active);
    }

    @Test
    void salaryPayments_allAndByEmployee_forTheMainFarmAccount() {
        SalaryPayment payment = SalaryPayment.builder().employeeId("e-1").month("2026-10").baseSalary(20000.0).build();
        when(farmData.salaryPayments()).thenReturn(List.of(payment));
        when(farmData.salaryPaymentsByEmployee("e-1")).thenReturn(List.of(payment));

        assertThat(service.listSalaryPayments()).containsExactly(payment);
        assertThat(service.listSalaryPaymentsByEmployee("e-1")).containsExactly(payment);
    }

    @Test
    void subTenants_cannotReadEmployeesOrSalaries_andNoApiCallIsMade() {
        TenantContext.setParentTenantId("root");

        for (Runnable call : List.<Runnable>of(service::listEmployees, service::listActiveEmployees,
                service::listSalaryPayments, () -> service.listSalaryPaymentsByEmployee("e-1"))) {
            assertThatThrownBy(call::run).isInstanceOf(ToolUserException.class)
                    .hasMessageContaining("only available to the main farm account");
        }
        verify(farmData, never()).employees();
        verify(farmData, never()).salaryPayments();
        verify(farmData, never()).salaryPaymentsByEmployee("e-1");
    }

    // ---- expenditures ----

    @Test
    void expenditures_allAndByBatch() {
        Expenditure e = Expenditure.builder().category("Feed").amount(100.0).batchId("b-1").build();
        when(farmData.expenditures()).thenReturn(List.of(e));
        when(resolver.resolveAnyBatch("pond 3")).thenReturn(new ResolvedBatch("b-1", "Sept", List.of("Pond 3"), null, "Active"));
        when(farmData.expendituresByBatch("b-1")).thenReturn(List.of(e));

        assertThat(service.listExpenditures()).containsExactly(e);
        assertThat(service.listExpendituresByBatch("pond 3")).containsExactly(e);
    }

    @Test
    void totalExpenditure_leavesOutSalaryRows_likeTheBatchFinancials() {
        when(resolver.resolveAnyBatch("pond 3")).thenReturn(new ResolvedBatch("b-1", "Sept", List.of("Pond 3"), null, "Active"));
        when(farmData.expendituresByBatch("b-1")).thenReturn(List.of(
                Expenditure.builder().category("Feed").amount(60000.0).build(),
                Expenditure.builder().category("Seed").amount(20000.0).build(),
                Expenditure.builder().category("Salary").amount(15000.0).build(),
                Expenditure.builder().category("Maintenance").amount(null).build()));

        BatchTotal total = service.getTotalExpenditureByBatch("pond 3");

        assertThat(total.total()).isEqualTo(80000.0);
        assertThat(total.records()).isEqualTo(4);
        assertThat(total.notes()).anyMatch(n -> n.contains("Salary") && n.contains("15000") && n.contains("farm-wide"))
                .anyMatch(n -> n.contains("no amount"));
    }
}
