package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.Expenditure;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.security.RootTenantOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only HR and expenditure tools. Employee and salary tools are limited to the main farm account
 * (see {@link RootTenantOnly}).
 */
@Service
@RequiredArgsConstructor
public class HRFinanceMcpService {

    private final FarmDataClient farmData;
    private final ResolverService resolver;

    // ---- Employees and salaries: main farm account only ----

    @Tool(name = "list_employees",
          description = "List all employees with role, salary, advance taken and status. Main farm account only.")
    public List<Employee> listEmployees() {
        RootTenantOnly.require();
        return farmData.employees();
    }

    @Tool(name = "list_active_employees",
          description = "List the active employees. Main farm account only.")
    public List<Employee> listActiveEmployees() {
        RootTenantOnly.require();
        return farmData.employees().stream().filter(e -> "Active".equalsIgnoreCase(e.getStatus())).toList();
    }

    @Tool(name = "list_salary_payments",
          description = "List all salary payments (month, base salary, advance deduction, bonus, net amount). Main farm account only.")
    public List<SalaryPayment> listSalaryPayments() {
        RootTenantOnly.require();
        return farmData.salaryPayments();
    }

    @Tool(name = "list_salary_payments_by_employee",
          description = "List the salary payments of one employee. Main farm account only.")
    public List<SalaryPayment> listSalaryPaymentsByEmployee(
            @ToolParam(description = "The employee id (from list_employees)") String employeeId) {
        RootTenantOnly.require();
        return farmData.salaryPaymentsByEmployee(employeeId);
    }

    // ---- Expenditures ----

    @Tool(name = "list_expenditures",
          description = "List all expenditures (date, category, macro investment, amount, description).")
    public List<Expenditure> listExpenditures() {
        return farmData.expenditures();
    }

    @Tool(name = "list_expenditures_by_batch",
          description = "List the expenditures tagged to one batch.")
    public List<Expenditure> listExpendituresByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        return farmData.expendituresByBatch(resolver.resolveAnyBatch(batchId).id());
    }

    @Tool(name = "get_total_expenditure_by_batch",
          description = "Total expenditure tagged to one batch, leaving out Salary entries (salaries are a farm-wide cost, "
                  + "not a batch cost). For the full picture use get_batch_financials.")
    public BatchTotal getTotalExpenditureByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        var batch = resolver.resolveAnyBatch(batchId);
        List<Expenditure> all = farmData.expendituresByBatch(batch.id());

        double total = 0;
        double salaryAmount = 0;
        int salaryRows = 0;
        int missingAmount = 0;
        for (Expenditure e : all) {
            boolean salary = e.getCategory() != null && "salary".equalsIgnoreCase(e.getCategory().trim());
            if (salary) {
                salaryRows++;
                salaryAmount += e.getAmount() == null ? 0 : e.getAmount();
            } else if (e.getAmount() == null) {
                missingAmount++;
            } else {
                total += e.getAmount();
            }
        }

        List<String> notes = new ArrayList<>();
        if (salaryRows > 0) {
            notes.add(salaryRows + " expenditure(s) of category Salary totalling " + plain(salaryAmount)
                    + " are tagged to this batch and left out: salaries are farm-wide, not batch costs.");
        }
        if (missingAmount > 0) {
            notes.add(missingAmount + " expenditure(s) have no amount and were ignored.");
        }
        return new BatchTotal(batch.id(), batch.name(), total, all.size(), notes);
    }

    private static String plain(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
