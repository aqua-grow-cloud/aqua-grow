package com.aquagrow.mcp.finance;

import com.aquagrow.mcp.finance.FinanceReports.*;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.Expenditure;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.model.FeedOrder;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.HarvestLog;
import com.aquagrow.mcp.model.OrderStatus;
import com.aquagrow.mcp.model.PaymentStatus;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.model.SampleLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    // ---- fixtures ----

    private static FishSeed seed(String status, Integer quantity, Double sizeGrams) {
        return FishSeed.builder().status(status).quantity(quantity).sizeGrams(sizeGrams).pondId("p1").fishTypeId("ft1").build();
    }

    /** A batch's status is derived from its seeds: all Harvested = Completed, otherwise Active. */
    private static Batch batch(FishSeed... seeds) {
        return Batch.builder().id("b1").name("Sept Katla").fishSeeds(List.of(seeds)).build();
    }

    private static HarvestLog harvest(String date, Double biomass, Double actual, Double revenue, Double commission) {
        return HarvestLog.builder().batchId("b1").date(LocalDate.parse(date)).biomassWeight(biomass)
                .actualWeight(actual).revenue(revenue).commission(commission).build();
    }

    private static Expenditure exp(String date, String category, String macro, Double amount, String batchId) {
        return Expenditure.builder().date(date == null ? null : LocalDate.parse(date)).category(category)
                .macroInvestment(macro).amount(amount).batchId(batchId).build();
    }

    private static SalaryPayment payment(String month, LocalDate date, double base, double bonus) {
        return SalaryPayment.builder().employeeId("e1").month(month).date(date).baseSalary(base).bonus(bonus)
                .advanceDeduction(0.0).netAmount(base + bonus).build();
    }

    // ---- batch financials ----

    @Test
    void batch_completedBatch_netRevenueCostMarginAndBreakEven() {
        Batch batch = batch(seed("Harvested", 1000, 50.0));
        List<HarvestLog> harvests = List.of(
                harvest("2026-09-20", 500.0, 475.0, 95000.0, 5000.0),
                harvest("2026-10-05", 500.0, 500.0, 100000.0, 0.0));
        List<Expenditure> exps = List.of(
                exp("2026-08-01", "Feed", "INVESTMENT", 60000.0, "b1"),
                exp("2026-08-05", "Seed", "INVESTMENT", 20000.0, "b1"),
                exp("2026-09-01", "Feed", "INVESTMENT", 40000.0, "b1"),
                exp("2026-09-30", "Salary", "NO_RETURN", 15000.0, "b1"),
                exp("2026-09-02", "Maintenance", "NO_RETURN", null, "b1"));

        BatchFinancials f = FinanceCalculator.batchFinancials(batch, exps, harvests, "INR");

        assertThat(f.provisional()).isFalse();
        assertThat(f.netRevenue()).isEqualTo(195000.0);
        assertThat(f.commission()).isEqualTo(5000.0);
        assertThat(f.grossSales()).isEqualTo(200000.0);
        assertThat(f.harvestedKg()).isEqualTo(1000.0);
        assertThat(f.billedKg()).isEqualTo(975.0);
        assertThat(f.netRatePerBilledKg()).isEqualTo(200.0);
        assertThat(f.totalDirectCost()).isEqualTo(120000.0);
        assertThat(f.costByCategory()).containsExactly(new CategoryAmount("Feed", 100000.0), new CategoryAmount("Seed", 20000.0));
        assertThat(f.profit()).isEqualTo(75000.0);
        assertThat(f.marginPercent()).isEqualTo(38.46);
        assertThat(f.roiPercent()).isEqualTo(62.5);
        assertThat(f.costPerKgHarvested()).isEqualTo(120.0);
        assertThat(f.breakEvenRatePerBilledKg()).isEqualTo(123.08);
        assertThat(f.currency()).isEqualTo("INR");
        assertThat(f.notes()).anyMatch(n -> n.contains("Salary") && n.contains("15000") && n.contains("farm-wide"));
        assertThat(f.notes()).anyMatch(n -> n.contains("no amount"));
        assertThat(f.notes()).noneMatch(n -> n.contains("provisional"));
    }

    @Test
    void batch_activeBatchIsProvisional() {
        Batch batch = batch(seed("Active", 1000, 50.0));

        BatchFinancials f = FinanceCalculator.batchFinancials(batch, List.of(exp("2026-08-01", "Seed", null, 1000.0, "b1")),
                List.of(harvest("2026-10-05", 100.0, 100.0, 10000.0, 0.0)), "INR");

        assertThat(f.provisional()).isTrue();
        assertThat(f.notes()).anyMatch(n -> n.contains("provisional"));
    }

    @Test
    void batch_noHarvests_profitIsMinusCostAndRatiosAreNull() {
        Batch batch = batch(seed("Active", 1000, 50.0));

        BatchFinancials f = FinanceCalculator.batchFinancials(batch, List.of(exp("2026-08-01", "Seed", null, 5000.0, "b1")),
                List.of(), "INR");

        assertThat(f.netRevenue()).isEqualTo(0.0);
        assertThat(f.profit()).isEqualTo(-5000.0);
        assertThat(f.marginPercent()).isNull();
        assertThat(f.roiPercent()).isEqualTo(-100.0);
        assertThat(f.costPerKgHarvested()).isNull();
        assertThat(f.breakEvenRatePerBilledKg()).isNull();
        assertThat(f.notes()).anyMatch(n -> n.contains("No harvests"));
    }

    @Test
    void batch_missingRevenueCountsAsZeroAndIsReported() {
        Batch batch = batch(seed("Harvested", 10, 10.0));

        BatchFinancials f = FinanceCalculator.batchFinancials(batch, List.of(),
                List.of(harvest("2026-10-05", 100.0, null, null, null)), "INR");

        assertThat(f.netRevenue()).isEqualTo(0.0);
        assertThat(f.billedKg()).isEqualTo(100.0); // falls back to biomass weight
        assertThat(f.notes()).anyMatch(n -> n.contains("no revenue recorded"));
    }

    // ---- farm P&L ----

    private static List<Expenditure> pnlExpenditures() {
        return List.of(
                exp("2026-09-02", "Feed", "INVESTMENT", 10000.0, "b1"),       // batch cost, Sept
                exp("2026-09-05", "Maintenance", "NO_RETURN", 3000.0, null),  // farm cost, Sept
                exp("2026-10-01", "Lease", "LEASE", 5000.0, null),            // farm cost, Oct
                exp("2026-08-01", "Feed", "INVESTMENT", 99999.0, "b1"),       // before range
                exp(null, "Feed", "INVESTMENT", 1.0, "b1"),                   // undated
                exp("2026-09-30", "Salary", "NO_RETURN", 18000.0, null),      // duplicate of Sept payment
                exp("2026-08-31", "Salary", "NO_RETURN", 7000.0, null));      // Aug has no payment record
    }

    private static List<HarvestLog> pnlHarvests() {
        return List.of(
                harvest("2026-09-20", 100.0, 100.0, 90000.0, 10000.0),
                harvest("2026-10-05", 50.0, 50.0, 45000.0, 5000.0),
                harvest("2026-08-10", 10.0, 10.0, 7777.0, 0.0));              // before range
    }

    @Test
    void pnl_monthlyRows_salariesNotDoubleCounted() {
        List<SalaryPayment> payments = List.of(payment("2026-09", LocalDate.of(2026, 9, 30), 20000, 2000));

        FarmPnl pnl = FinanceCalculator.farmPnl(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 10),
                pnlExpenditures(), pnlHarvests(), payments, true, "INR");

        assertThat(pnl.salariesIncluded()).isTrue();
        assertThat(pnl.months()).extracting(MonthPnl::month).containsExactly("2026-08", "2026-09", "2026-10");

        MonthPnl aug = pnl.months().get(0);
        assertThat(aug.salaries()).isEqualTo(7000.0);          // from the Salary expenditure: no payment record in Aug
        assertThat(aug.netRevenue()).isEqualTo(0.0);
        assertThat(aug.profit()).isEqualTo(-7000.0);

        MonthPnl sep = pnl.months().get(1);
        assertThat(sep.grossSales()).isEqualTo(100000.0);
        assertThat(sep.commission()).isEqualTo(10000.0);
        assertThat(sep.netRevenue()).isEqualTo(90000.0);
        assertThat(sep.batchCosts()).isEqualTo(10000.0);
        assertThat(sep.farmCosts()).isEqualTo(3000.0);
        assertThat(sep.salaries()).isEqualTo(22000.0);         // base + bonus; the 18000 expenditure is ignored
        assertThat(sep.totalCosts()).isEqualTo(35000.0);
        assertThat(sep.profit()).isEqualTo(55000.0);

        MonthPnl oct = pnl.months().get(2);
        assertThat(oct.netRevenue()).isEqualTo(45000.0);
        assertThat(oct.farmCosts()).isEqualTo(5000.0);
        assertThat(oct.salaries()).isEqualTo(0.0);
        assertThat(oct.profit()).isEqualTo(40000.0);

        MonthPnl total = pnl.total();
        assertThat(total.netRevenue()).isEqualTo(135000.0);
        assertThat(total.batchCosts()).isEqualTo(10000.0);
        assertThat(total.farmCosts()).isEqualTo(8000.0);
        assertThat(total.salaries()).isEqualTo(29000.0);
        assertThat(total.totalCosts()).isEqualTo(47000.0);
        assertThat(total.profit()).isEqualTo(88000.0);

        assertThat(pnl.costByCategory()).containsExactly(
                new CategoryAmount("Salary", 29000.0), new CategoryAmount("Feed", 10000.0),
                new CategoryAmount("Lease", 5000.0), new CategoryAmount("Maintenance", 3000.0));
        assertThat(pnl.costByMacroInvestment()).containsExactly(
                new CategoryAmount("SALARY", 29000.0), new CategoryAmount("INVESTMENT", 10000.0),
                new CategoryAmount("LEASE", 5000.0), new CategoryAmount("NO_RETURN", 3000.0));
        assertThat(pnl.notes()).anyMatch(n -> n.contains("18000") && n.contains("twice"));
        assertThat(pnl.notes()).anyMatch(n -> n.contains("no date"));
        assertThat(pnl.notes()).anyMatch(n -> n.contains("net of commission"));
        assertThat(pnl.notes()).anyMatch(n -> n.contains("feed orders carry no price"));
    }

    @Test
    void pnl_withoutSalaryAccess_excludesSalariesAndSaysSo() {
        List<SalaryPayment> payments = List.of(payment("2026-09", LocalDate.of(2026, 9, 30), 20000, 2000));

        FarmPnl pnl = FinanceCalculator.farmPnl(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 10),
                pnlExpenditures(), pnlHarvests(), payments, false, "INR");

        assertThat(pnl.salariesIncluded()).isFalse();
        assertThat(pnl.months()).allSatisfy(m -> assertThat(m.salaries()).isNull());
        assertThat(pnl.total().salaries()).isNull();
        assertThat(pnl.months().get(1).totalCosts()).isEqualTo(13000.0);
        assertThat(pnl.months().get(1).profit()).isEqualTo(77000.0);
        assertThat(pnl.costByCategory()).extracting(CategoryAmount::category).doesNotContain("Salary");
        assertThat(pnl.notes()).anyMatch(n -> n.contains("EXCLUDES salaries"));
    }

    @Test
    void pnl_salaryPaymentMonthFallsBackToPaymentDate_andRespectsRange() {
        List<SalaryPayment> payments = List.of(
                payment(null, LocalDate.of(2026, 9, 28), 10000, 0),     // month from date
                payment("2026-07", LocalDate.of(2026, 7, 31), 99999, 0), // outside range
                payment("garbage", null, 5, 0));                          // unusable: ignored

        FarmPnl pnl = FinanceCalculator.farmPnl(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                List.of(), List.of(), payments, true, "INR");

        assertThat(pnl.months()).hasSize(1);
        assertThat(pnl.months().get(0).salaries()).isEqualTo(10000.0);
    }

    @Test
    void pnl_emptyPeriod_isAllZerosWithANote() {
        FarmPnl pnl = FinanceCalculator.farmPnl(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                List.of(), List.of(), List.of(), true, "INR");

        assertThat(pnl.total().profit()).isEqualTo(0.0);
        assertThat(pnl.notes()).anyMatch(n -> n.contains("No salary payments"));
    }

    // ---- feed efficiency ----

    private static FeedLog feed(String date, String type, double kg) {
        return FeedLog.builder().batchId("b1").date(LocalDate.parse(date)).feedType(type).amount(kg).build();
    }

    private static SampleLog sample(String date, double abw) {
        return SampleLog.builder().batchId("b1").date(LocalDate.parse(date)).averageWeight(abw).build();
    }

    @Test
    void feedEfficiency_completedBatch_fcrAndDailyAverage() {
        Batch batch = batch(seed("Harvested", 1000, 50.0)); // 50 kg stocked

        FeedEfficiency f = FinanceCalculator.feedEfficiency(batch,
                List.of(feed("2026-10-01", "Starter", 400), feed("2026-10-02", "Starter", 400), feed("2026-10-04", "Grower", 625)),
                List.of(exp("2026-09-01", "feed", "INVESTMENT", 57000.0, "b1")),
                List.of(harvest("2026-10-10", 1000.0, 1000.0, 1.0, 0.0)),
                List.of(sample("2026-10-01", 40), sample("2026-10-10", 1000)), TODAY, "INR");

        assertThat(f.totalFeedKg()).isEqualTo(1425.0);
        assertThat(f.feedByType()).containsExactly(new CategoryAmount("Starter", 800.0), new CategoryAmount("Grower", 625.0));
        assertThat(f.firstFeedDate()).isEqualTo("2026-10-01");
        assertThat(f.lastFeedDate()).isEqualTo("2026-10-04");
        assertThat(f.daysWithFeed()).isEqualTo(3);
        assertThat(f.averageDailyFeedKg()).isEqualTo(356.25);
        assertThat(f.stockedBiomassKg()).isEqualTo(50.0);
        assertThat(f.biomassGainKg()).isEqualTo(950.0);
        assertThat(f.fcr()).isEqualTo(1.5);
        assertThat(f.feedPerKgHarvested()).isEqualTo(1.43);
        assertThat(f.feedExpenditure()).isEqualTo(57000.0);
        assertThat(f.feedCostPerKgHarvested()).isEqualTo(57.0);
        assertThat(f.latestAverageWeightGrams()).isEqualTo(1000.0);
        assertThat(f.latestSampleDate()).isEqualTo("2026-10-10");
        assertThat(f.notes()).anyMatch(n -> n.contains("1 day(s)") && n.contains("no feed"));
        assertThat(f.notes()).noneMatch(n -> n.contains("growth sample"));
    }

    @Test
    void feedEfficiency_activeBatchHasNoFcrYet() {
        Batch batch = batch(seed("Active", 1000, 50.0));

        FeedEfficiency f = FinanceCalculator.feedEfficiency(batch, List.of(feed("2026-10-01", "Starter", 400)),
                List.of(), List.of(harvest("2026-10-10", 100.0, 100.0, 1.0, 0.0)), List.of(), TODAY, "INR");

        assertThat(f.fcr()).isNull();
        assertThat(f.feedPerKgHarvested()).isEqualTo(4.0);
        assertThat(f.notes()).anyMatch(n -> n.contains("FCR needs the final harvest"));
        assertThat(f.notes()).anyMatch(n -> n.contains("growth sample"));
        assertThat(f.notes()).anyMatch(n -> n.contains("category Feed"));
    }

    @Test
    void feedEfficiency_missingSeedSizeMeansNoFcr() {
        Batch batch = batch(seed("Harvested", 1000, null));

        FeedEfficiency f = FinanceCalculator.feedEfficiency(batch, List.of(feed("2026-10-01", "Starter", 400)),
                List.of(), List.of(harvest("2026-10-10", 100.0, 100.0, 1.0, 0.0)), List.of(), TODAY, "INR");

        assertThat(f.stockedBiomassKg()).isNull();
        assertThat(f.fcr()).isNull();
        assertThat(f.notes()).anyMatch(n -> n.contains("quantity or size is missing"));
    }

    @Test
    void feedEfficiency_noFeedLogs() {
        FeedEfficiency f = FinanceCalculator.feedEfficiency(batch(seed("Active", 1, 1.0)), List.of(), List.of(),
                List.of(), List.of(), TODAY, "INR");

        assertThat(f.totalFeedKg()).isEqualTo(0.0);
        assertThat(f.averageDailyFeedKg()).isNull();
        assertThat(f.notes()).anyMatch(n -> n.contains("No feed logs"));
    }

    // ---- payables ----

    private static FeedOrder order(String id, OrderStatus status, PaymentStatus pay, double kg, String supplier) {
        return FeedOrder.builder().id(id).feedType("King Fish Feed").quantity(kg).status(status).paymentStatus(pay)
                .supplierId(supplier).orderDate(LocalDateTime.of(2026, 10, 1, 9, 0)).build();
    }

    private static Employee employee(String id, String name, String status, double salary, double advance) {
        return Employee.builder().id(id).name(name).role("Worker").status(status).salary(salary).advanceTaken(advance).build();
    }

    @Test
    void payables_unpaidOrdersByQuantityAndEmployeesNotYetPaid() {
        List<FeedOrder> orders = List.of(
                order("o1", OrderStatus.PENDING, PaymentStatus.UNPAID, 500, "s1"),
                order("o2", OrderStatus.RECEIVED, PaymentStatus.PARTIAL, 1000, "s1"),
                order("o3", OrderStatus.RECEIVED, PaymentStatus.PAID, 700, "s1"),
                order("o4", OrderStatus.CANCELLED, PaymentStatus.UNPAID, 300, "s1"));
        List<Employee> employees = List.of(
                employee("e1", "Ravi", "Active", 20000, 0),
                employee("e2", "Sita", "Active", 15000, 5000),
                employee("e3", "Old Hand", "Inactive", 12000, 3000));
        List<SalaryPayment> payments = List.of(
                SalaryPayment.builder().employeeId("e1").month("2026-10").baseSalary(20000.0).build(),
                SalaryPayment.builder().employeeId("e2").month("2026-09").baseSalary(15000.0).build());

        Payables p = FinanceCalculator.payables(TODAY, YearMonth.of(2026, 10), orders, Map.of("s1", "Agro Feeds"),
                employees, payments, true, "INR");

        assertThat(p.feedOrders().count()).isEqualTo(2);
        assertThat(p.feedOrders().totalQuantityKg()).isEqualTo(1500.0);
        assertThat(p.feedOrders().amountsRecorded()).isFalse();
        assertThat(p.feedOrders().orders()).extracting(UnpaidFeedOrder::orderId).containsExactly("o1", "o2");
        assertThat(p.feedOrders().orders().get(0).supplier()).isEqualTo("Agro Feeds");
        assertThat(p.feedOrders().orders().get(1).paymentStatus()).isEqualTo("PARTIAL");
        assertThat(p.salaries().month()).isEqualTo("2026-10");
        assertThat(p.salaries().notYetPaid()).extracting(EmployeeDue::name).containsExactly("Sita");
        assertThat(p.salaries().totalExpectedBaseSalary()).isEqualTo(15000.0);
        assertThat(p.salaries().advanceBalances()).extracting(AdvanceBalance::name).containsExactly("Sita");
        assertThat(p.salaries().totalAdvanceOutstanding()).isEqualTo(5000.0);
        assertThat(p.notes()).anyMatch(n -> n.contains("no price recorded"));
    }

    @Test
    void payables_withoutSalaryAccess_hasNoSalarySection() {
        Payables p = FinanceCalculator.payables(TODAY, YearMonth.of(2026, 10), List.of(), Map.of(),
                List.of(employee("e1", "Ravi", "Active", 20000, 100)), List.of(), false, "INR");

        assertThat(p.salaries()).isNull();
        assertThat(p.feedOrders().count()).isEqualTo(0);
        assertThat(p.notes()).anyMatch(n -> n.contains("only available to the main farm account"));
    }

    @Test
    void payables_futureMonthIsFlagged() {
        Payables p = FinanceCalculator.payables(TODAY, YearMonth.of(2026, 11), List.of(), Map.of(),
                List.of(employee("e1", "Ravi", "Active", 20000, 0)), List.of(), true, "INR");

        assertThat(p.notes()).anyMatch(n -> n.contains("has not started yet"));
    }
}
