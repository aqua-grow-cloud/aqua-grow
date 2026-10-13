package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.finance.FinanceReports.BatchFinancials;
import com.aquagrow.mcp.finance.FinanceReports.FarmPnl;
import com.aquagrow.mcp.finance.FinanceReports.FeedEfficiency;
import com.aquagrow.mcp.finance.FinanceReports.Payables;
import com.aquagrow.mcp.mcp.FarmConventions;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.Expenditure;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.HarvestLog;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceMcpServiceTest {

    private static final String TENANT = "t1";

    @Mock ResolverService resolver;
    @Mock FarmDataClient farmData;
    @Mock FarmConventions conventions;

    private FinanceMcpService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        service = new FinanceMcpService(resolver, farmData, conventions, clock);
        TenantContext.setTenantId(TENANT);
        lenient().when(conventions.currency()).thenReturn("INR");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Batch givenBatch(String seedStatus) {
        Batch batch = Batch.builder().id("b-1").name("Sept Katla").tenantId(TENANT)
                .fishSeeds(List.of(FishSeed.builder().status(seedStatus).quantity(1000).sizeGrams(50.0).build())).build();
        lenient().when(resolver.resolveAnyBatch("pond 3"))
                .thenReturn(new ResolvedBatch("b-1", "Sept Katla", List.of("Pond 3"), LocalDate.of(2026, 9, 1), batch.getStatus()));
        lenient().when(farmData.batch("b-1")).thenReturn(batch);
        return batch;
    }

    private static HarvestLog harvest(double revenue, double commission, double kg) {
        return HarvestLog.builder().batchId("b-1").date(LocalDate.of(2026, 10, 5)).biomassWeight(kg).actualWeight(kg)
                .revenue(revenue).commission(commission).build();
    }

    // ---- batch ----

    @Test
    void batchFinancials_readsTheResolvedBatchesData_andNothingElse() {
        givenBatch("Harvested");
        when(farmData.expendituresByBatch("b-1")).thenReturn(List.of(
                Expenditure.builder().category("Feed").amount(60000.0).batchId("b-1").build(),
                Expenditure.builder().category("Salary").amount(9999.0).batchId("b-1").build()));
        when(farmData.harvestLogsByBatch("b-1")).thenReturn(List.of(harvest(95000.0, 5000.0, 500.0)));

        BatchFinancials f = service.getBatchFinancials("pond 3");

        assertThat(f.batchName()).isEqualTo("Sept Katla");
        assertThat(f.currency()).isEqualTo("INR");
        assertThat(f.netRevenue()).isEqualTo(95000.0);
        assertThat(f.totalDirectCost()).isEqualTo(60000.0);
        assertThat(f.profit()).isEqualTo(35000.0);
        assertThat(f.provisional()).isFalse();
        verify(farmData, never()).salaryPayments();
        verify(farmData, never()).employees();
    }

    @Test
    void batchFinancials_aBatchTheClientHidesIsNotFound() {
        when(resolver.resolveAnyBatch("x")).thenReturn(new ResolvedBatch("b-1", "Foreign", List.of(), null, "Unknown"));
        when(farmData.batch("b-1")).thenThrow(new ToolUserException("Batch not found: b-1"));

        assertThatThrownBy(() -> service.getBatchFinancials("x"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("Batch not found");
    }

    // ---- P&L ----

    @Test
    void farmPnl_rootTenantSeesSalaries() {
        when(farmData.expenditures()).thenReturn(List.of());
        when(farmData.harvestLogs()).thenReturn(List.of(harvest(1000.0, 0.0, 10.0)));
        when(farmData.salaryPayments()).thenReturn(List.of(
                SalaryPayment.builder().employeeId("e1").month("2026-10").baseSalary(300.0).bonus(100.0).build()));

        FarmPnl pnl = service.getFarmPnl("2026-10-01", "2026-10-31");

        assertThat(pnl.salariesIncluded()).isTrue();
        assertThat(pnl.total().salaries()).isEqualTo(400.0);
        assertThat(pnl.total().profit()).isEqualTo(600.0);
    }

    @Test
    void farmPnl_subTenantNeverReadsSalaryData() {
        TenantContext.setParentTenantId("root");
        when(farmData.expenditures()).thenReturn(List.of());
        when(farmData.harvestLogs()).thenReturn(List.of(harvest(1000.0, 0.0, 10.0)));

        FarmPnl pnl = service.getFarmPnl("2026-10-01", "2026-10-31");

        assertThat(pnl.salariesIncluded()).isFalse();
        assertThat(pnl.total().salaries()).isNull();
        assertThat(pnl.notes()).anyMatch(n -> n.contains("EXCLUDES salaries"));
        verify(farmData, never()).salaryPayments();
    }

    @Test
    void farmPnl_validatesThePeriodBeforeCallingTheApi() {
        assertThatThrownBy(() -> service.getFarmPnl("2026-10-31", "2026-10-01"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("before");
        assertThatThrownBy(() -> service.getFarmPnl("2026-10-01", null)).isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.getFarmPnl("10/01/2026", "2026-10-31"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("YYYY-MM-DD");
        assertThatThrownBy(() -> service.getFarmPnl("2022-01-01", "2026-10-31"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("36 months");
        verify(farmData, never()).expenditures();
    }

    @Test
    void farmPnl_thirtySixMonthsIsTheLimit() {
        when(farmData.expenditures()).thenReturn(List.of());
        when(farmData.harvestLogs()).thenReturn(List.of());
        when(farmData.salaryPayments()).thenReturn(List.of());

        assertThat(service.getFarmPnl("2023-11-01", "2026-10-31").months()).hasSize(36);
    }

    // ---- payables ----

    @Test
    void payables_defaultsToTheCurrentMonthAndShowsSalariesToRoot() {
        when(farmData.feedOrders()).thenReturn(List.of());
        when(farmData.suppliers()).thenReturn(List.of());
        when(farmData.employees()).thenReturn(List.of(
                Employee.builder().id("e1").name("Ravi").role("Worker").status("Active").salary(20000.0).advanceTaken(0.0).build()));
        when(farmData.salaryPayments()).thenReturn(List.of());

        Payables p = service.getOutstandingPayables(null);

        assertThat(p.asOf()).isEqualTo("2026-10-13");
        assertThat(p.salaries().month()).isEqualTo("2026-10");
        assertThat(p.salaries().notYetPaid()).hasSize(1);
    }

    @Test
    void payables_subTenantNeverReadsEmployeesOrSalaries() {
        TenantContext.setParentTenantId("root");
        when(farmData.feedOrders()).thenReturn(List.of());
        when(farmData.suppliers()).thenReturn(List.of());

        Payables p = service.getOutstandingPayables("2026-09");

        assertThat(p.salaries()).isNull();
        verify(farmData, never()).employees();
        verify(farmData, never()).salaryPayments();
    }

    @Test
    void payables_rejectsABadMonthBeforeCallingTheApi() {
        assertThatThrownBy(() -> service.getOutstandingPayables("October"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("YYYY-MM");
        verify(farmData, never()).feedOrders();
    }

    // ---- feed efficiency ----

    @Test
    void feedEfficiency_wiresTheBatchData() {
        givenBatch("Harvested");
        when(farmData.feedLogsByBatch("b-1")).thenReturn(List.of(
                FeedLog.builder().batchId("b-1").date(LocalDate.of(2026, 10, 1)).feedType("Starter").amount(75.0).build()));
        when(farmData.expendituresByBatch("b-1")).thenReturn(List.of());
        when(farmData.harvestLogsByBatch("b-1")).thenReturn(List.of(harvest(1.0, 0.0, 100.0)));
        when(farmData.sampleLogsByBatch("b-1")).thenReturn(List.of());

        FeedEfficiency f = service.getFeedEfficiency("pond 3");

        assertThat(f.totalFeedKg()).isEqualTo(75.0);
        assertThat(f.stockedBiomassKg()).isEqualTo(50.0);
        assertThat(f.fcr()).isEqualTo(1.5);   // 75 kg feed / (100 - 50) kg gain
    }
}
