package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.finance.FinanceCalculator;
import com.aquagrow.mcp.finance.FinanceReports.BatchFinancials;
import com.aquagrow.mcp.finance.FinanceReports.FarmPnl;
import com.aquagrow.mcp.finance.FinanceReports.FeedEfficiency;
import com.aquagrow.mcp.finance.FinanceReports.Payables;
import com.aquagrow.mcp.mcp.FarmConventions;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.model.Supplier;
import com.aquagrow.mcp.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only financial analysis. All arithmetic is done here, not by the model; every
 * result carries notes about data quality and the assumptions behind the numbers.
 * Salary data is limited to the main (root) farm account.
 */
@Service
@RequiredArgsConstructor
public class FinanceMcpService {

    private static final int MAX_PNL_MONTHS = 36;

    private final ResolverService resolver;
    private final FarmDataClient farmData;
    private final FarmConventions conventions;
    private final Clock clock;

    @Tool(name = "get_batch_financials",
          description = "Profit and cost analysis for ONE batch (active or completed): net revenue (after commission), "
                  + "direct costs by category, profit, margin %, ROI %, cost per kg harvested and the break-even rate "
                  + "per kg. Salaries and other farm-wide costs are NOT allocated to batches. Batches that are not "
                  + "fully harvested are marked provisional. Quote the numbers as returned and pass on the notes.")
    public BatchFinancials getBatchFinancials(
            @ToolParam(description = "Batch name, batch id, or pond name (e.g. 'Pond 3')") String batch) {
        Batch found = loadBatch(batch);
        return FinanceCalculator.batchFinancials(found,
                farmData.expendituresByBatch(found.getId()),
                farmData.harvestLogsByBatch(found.getId()),
                conventions.currency());
    }

    @Tool(name = "get_farm_pnl",
          description = "Farm-wide profit and loss for a date range, month by month: gross sales, commission, net "
                  + "revenue, batch costs, farm-level costs, salaries, total costs and profit, plus cost breakdowns by "
                  + "category and by macro investment (ADVANCE, LEASE, INVESTMENT, NO_RETURN). Items are counted on "
                  + "their own dates, so this is not the same as a batch's profit. Salaries are only shown to the main "
                  + "farm account; for others the result says profit excludes them. Quote the numbers as returned and "
                  + "pass on the notes.")
    public FarmPnl getFarmPnl(
            @ToolParam(description = "First day of the period, YYYY-MM-DD") String from,
            @ToolParam(description = "Last day of the period, YYYY-MM-DD") String to) {
        LocalDate start = parseDate(from, "from");
        LocalDate end = parseDate(to, "to");
        if (end.isBefore(start)) {
            throw new ToolUserException("'to' (" + end + ") is before 'from' (" + start + ").");
        }
        if (ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1 > MAX_PNL_MONTHS) {
            throw new ToolUserException("The period covers more than " + MAX_PNL_MONTHS + " months. Use a shorter period.");
        }
        boolean salaries = TenantContext.isRootTenant();
        return FinanceCalculator.farmPnl(start, end,
                farmData.expenditures(),
                farmData.harvestLogs(),
                salaries ? farmData.salaryPayments() : null,
                salaries, conventions.currency());
    }

    @Tool(name = "get_outstanding_payables",
          description = "What is still owed: unpaid or part-paid feed orders (quantities only, because feed orders have "
                  + "no price recorded) and, for the main farm account, active employees with no salary payment "
                  + "recorded for the month plus outstanding employee advance balances. Do not invent rupee amounts for "
                  + "feed orders.")
    public Payables getOutstandingPayables(
            @ToolParam(description = "Salary month, YYYY-MM. Defaults to the current month.", required = false) String month) {
        YearMonth salaryMonth = month == null || month.isBlank() ? YearMonth.now(clock) : parseMonth(month);
        boolean salaries = TenantContext.isRootTenant();

        Map<String, String> supplierNames = farmData.suppliers().stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName, (a, b) -> a));
        List<Employee> employees = salaries ? farmData.employees() : List.of();
        List<SalaryPayment> payments = salaries ? farmData.salaryPayments() : List.of();

        return FinanceCalculator.payables(LocalDate.now(clock), salaryMonth,
                farmData.feedOrders(), supplierNames, employees, payments,
                salaries, conventions.currency());
    }

    @Tool(name = "get_feed_efficiency",
          description = "Feed efficiency for ONE batch: total feed used (by type), average daily feed, harvested weight, "
                  + "FCR (only once the batch is fully harvested and stocking weights are known), feed per kg "
                  + "harvested, feed spend from Feed expenditures and the latest growth sample. Quote the numbers as "
                  + "returned and pass on the notes about missing data.")
    public FeedEfficiency getFeedEfficiency(
            @ToolParam(description = "Batch name, batch id, or pond name (e.g. 'Pond 3')") String batch) {
        Batch found = loadBatch(batch);
        return FinanceCalculator.feedEfficiency(found,
                farmData.feedLogsByBatch(found.getId()),
                farmData.expendituresByBatch(found.getId()),
                farmData.harvestLogsByBatch(found.getId()),
                farmData.sampleLogsByBatch(found.getId()),
                LocalDate.now(clock), conventions.currency());
    }

    private Batch loadBatch(String reference) {
        var resolved = resolver.resolveAnyBatch(reference);
        return farmData.batch(resolved.id());   // the client hides batches that belong to another farm
    }

    private static LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ToolUserException("'" + field + "' is required (format YYYY-MM-DD).");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ToolUserException("'" + value + "' is not a valid date for '" + field + "'. Use the format YYYY-MM-DD.");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ToolUserException("'" + value + "' is not a valid month. Use the format YYYY-MM.");
        }
    }
}
