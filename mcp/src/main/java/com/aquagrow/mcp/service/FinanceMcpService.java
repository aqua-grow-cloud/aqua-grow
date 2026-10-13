package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.config.TenantFilterActivator;
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
import com.aquagrow.mcp.repository.BatchRepository;
import com.aquagrow.mcp.repository.EmployeeRepository;
import com.aquagrow.mcp.repository.ExpenditureRepository;
import com.aquagrow.mcp.repository.FeedLogRepository;
import com.aquagrow.mcp.repository.FeedOrderRepository;
import com.aquagrow.mcp.repository.HarvestLogRepository;
import com.aquagrow.mcp.repository.SalaryPaymentRepository;
import com.aquagrow.mcp.repository.SampleLogRepository;
import com.aquagrow.mcp.repository.SupplierRepository;
import com.aquagrow.mcp.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TenantFilterActivator tenantFilterActivator;
    private final BatchRepository batchRepository;
    private final ExpenditureRepository expenditureRepository;
    private final HarvestLogRepository harvestLogRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final FeedOrderRepository feedOrderRepository;
    private final SupplierRepository supplierRepository;
    private final FeedLogRepository feedLogRepository;
    private final SampleLogRepository sampleLogRepository;
    private final FarmConventions conventions;
    private final Clock clock;

    @Tool(name = "get_batch_financials",
          description = "Profit and cost analysis for ONE batch (active or completed): net revenue (after commission), "
                  + "direct costs by category, profit, margin %, ROI %, cost per kg harvested and the break-even rate "
                  + "per kg. Salaries and other farm-wide costs are NOT allocated to batches. Batches that are not "
                  + "fully harvested are marked provisional. Quote the numbers as returned and pass on the notes.")
    @Transactional(readOnly = true)
    public BatchFinancials getBatchFinancials(
            @ToolParam(description = "Batch name, batch id, or pond name (e.g. 'Pond 3')") String batch) {
        Batch found = loadBatch(batch);
        String tenantId = TenantContext.getTenantId();
        return FinanceCalculator.batchFinancials(found,
                expenditureRepository.findByTenantIdAndBatchId(tenantId, found.getId()),
                harvestLogRepository.findByTenantIdAndBatchId(tenantId, found.getId()),
                conventions.currency());
    }

    @Tool(name = "get_farm_pnl",
          description = "Farm-wide profit and loss for a date range, month by month: gross sales, commission, net "
                  + "revenue, batch costs, farm-level costs, salaries, total costs and profit, plus cost breakdowns by "
                  + "category and by macro investment (ADVANCE, LEASE, INVESTMENT, NO_RETURN). Items are counted on "
                  + "their own dates, so this is not the same as a batch's profit. Salaries are only shown to the main "
                  + "farm account; for others the result says profit excludes them. Quote the numbers as returned and "
                  + "pass on the notes.")
    @Transactional(readOnly = true)
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
        String tenantId = tenantFilterActivator.activate();
        boolean salaries = TenantContext.isRootTenant();
        return FinanceCalculator.farmPnl(start, end,
                expenditureRepository.findByTenantId(tenantId),
                harvestLogRepository.findByTenantId(tenantId),
                salaries ? salaryPaymentRepository.findByTenantId(tenantId) : null,
                salaries, conventions.currency());
    }

    @Tool(name = "get_outstanding_payables",
          description = "What is still owed: unpaid or part-paid feed orders (quantities only, because feed orders have "
                  + "no price recorded) and, for the main farm account, active employees with no salary payment "
                  + "recorded for the month plus outstanding employee advance balances. Do not invent rupee amounts for "
                  + "feed orders.")
    @Transactional(readOnly = true)
    public Payables getOutstandingPayables(
            @ToolParam(description = "Salary month, YYYY-MM. Defaults to the current month.", required = false) String month) {
        YearMonth salaryMonth = month == null || month.isBlank() ? YearMonth.now(clock) : parseMonth(month);
        String tenantId = tenantFilterActivator.activate();
        boolean salaries = TenantContext.isRootTenant();

        Map<String, String> supplierNames = supplierRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName, (a, b) -> a));
        List<Employee> employees = salaries ? employeeRepository.findByTenantId(tenantId) : List.of();
        List<SalaryPayment> payments = salaries ? salaryPaymentRepository.findByTenantId(tenantId) : List.of();

        return FinanceCalculator.payables(LocalDate.now(clock), salaryMonth,
                feedOrderRepository.findByTenantId(tenantId), supplierNames, employees, payments,
                salaries, conventions.currency());
    }

    @Tool(name = "get_feed_efficiency",
          description = "Feed efficiency for ONE batch: total feed used (by type), average daily feed, harvested weight, "
                  + "FCR (only once the batch is fully harvested and stocking weights are known), feed per kg "
                  + "harvested, feed spend from Feed expenditures and the latest growth sample. Quote the numbers as "
                  + "returned and pass on the notes about missing data.")
    @Transactional(readOnly = true)
    public FeedEfficiency getFeedEfficiency(
            @ToolParam(description = "Batch name, batch id, or pond name (e.g. 'Pond 3')") String batch) {
        Batch found = loadBatch(batch);
        String tenantId = TenantContext.getTenantId();
        return FinanceCalculator.feedEfficiency(found,
                feedLogRepository.findByTenantIdAndBatchId(tenantId, found.getId()),
                expenditureRepository.findByTenantIdAndBatchId(tenantId, found.getId()),
                harvestLogRepository.findByTenantIdAndBatchId(tenantId, found.getId()),
                sampleLogRepository.findByTenantIdAndBatchId(tenantId, found.getId()),
                LocalDate.now(clock), conventions.currency());
    }

    private Batch loadBatch(String reference) {
        var resolved = resolver.resolveAnyBatch(reference);
        String tenantId = tenantFilterActivator.activate();
        return batchRepository.findById(resolved.id())
                .filter(b -> tenantId.equals(b.getTenantId()))
                .orElseThrow(() -> new ToolUserException("Batch not found: " + resolved.id()));
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
