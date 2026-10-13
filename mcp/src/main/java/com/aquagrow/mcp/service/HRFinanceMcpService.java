package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.model.Expenditure;
import com.aquagrow.mcp.repository.EmployeeRepository;
import com.aquagrow.mcp.repository.SalaryPaymentRepository;
import com.aquagrow.mcp.repository.ExpenditureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP Tools for HR and Finance (employees, salary payments, expenditures).
 * Tenant scoping is derived from the authenticated caller's JWT
 * (see TenantFilter/TenantContext) — never from an LLM-supplied parameter.
 */
@Service
@RequiredArgsConstructor
public class HRFinanceMcpService {

    private final EmployeeRepository employeeRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final ExpenditureRepository expenditureRepository;
    private final TenantFilterActivator tenantFilterActivator;

    // ---- Employees ----

    @Tool(name = "list_employees",
          description = "List all employees for the current tenant including role, salary, and status.")
    @Transactional(readOnly = true)
    public List<Employee> listEmployees() {
        String tenantId = tenantFilterActivator.activate();
        return employeeRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_active_employees",
          description = "List all active employees for the current tenant.")
    @Transactional(readOnly = true)
    public List<Employee> listActiveEmployees() {
        String tenantId = tenantFilterActivator.activate();
        return employeeRepository.findByTenantIdAndStatus(tenantId, "Active");
    }

    @Tool(name = "create_employee",
          description = "Create a new employee record. Provide name, role, salary, joinedDate (YYYY-MM-DD), and optional whatsappNumber.")
    @Transactional
    public Employee createEmployee(String name, String role, Double salary,
                                    String joinedDate, String whatsappNumber) {
        String tenantId = tenantFilterActivator.activate();
        Employee employee = Employee.builder()
            .tenantId(tenantId)
            .name(name)
            .role(role)
            .salary(salary)
            .status("Active")
            .joinedDate(joinedDate != null ? LocalDate.parse(joinedDate) : null)
            .whatsappNumber(whatsappNumber)
            .advanceTaken(0.0)
            .build();
        return employeeRepository.save(employee);
    }

    @Tool(name = "update_employee_advance",
          description = "Update the advance taken amount for an employee.")
    @Transactional
    public Employee updateEmployeeAdvance(String employeeId, Double advanceTaken) {
        String tenantId = tenantFilterActivator.activate();
        Employee employee = employeeRepository.findById(employeeId)
            .filter(e -> tenantId.equals(e.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        employee.setAdvanceTaken(advanceTaken);
        return employeeRepository.save(employee);
    }

    // ---- Salary Payments ----

    @Tool(name = "list_salary_payments",
          description = "List all salary payment records for the current tenant.")
    @Transactional(readOnly = true)
    public List<SalaryPayment> listSalaryPayments() {
        String tenantId = tenantFilterActivator.activate();
        return salaryPaymentRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_salary_payments_by_employee",
          description = "List salary payment history for a specific employee.")
    @Transactional(readOnly = true)
    public List<SalaryPayment> listSalaryPaymentsByEmployee(String employeeId) {
        String tenantId = tenantFilterActivator.activate();
        return salaryPaymentRepository.findByTenantIdAndEmployeeId(tenantId, employeeId);
    }

    @Tool(name = "create_salary_payment",
          description = "Record a salary payment. Provide employeeId, date (YYYY-MM-DD), baseSalary, advanceDeduction, bonus, netAmount, and month (YYYY-MM).")
    @Transactional
    public SalaryPayment createSalaryPayment(String employeeId, String date,
                                              Double baseSalary, Double advanceDeduction,
                                              Double bonus, Double netAmount, String month) {
        String tenantId = tenantFilterActivator.activate();
        SalaryPayment payment = SalaryPayment.builder()
            .tenantId(tenantId)
            .employeeId(employeeId)
            .date(LocalDate.parse(date))
            .baseSalary(baseSalary)
            .advanceDeduction(advanceDeduction)
            .bonus(bonus)
            .netAmount(netAmount)
            .month(month)
            .build();
        return salaryPaymentRepository.save(payment);
    }

    // ---- Expenditures ----

    @Tool(name = "list_expenditures",
          description = "List all expenditures for the current tenant. Categories include Feed, Seed, Salary, Maintenance, Lease, etc.")
    @Transactional(readOnly = true)
    public List<Expenditure> listExpenditures() {
        String tenantId = tenantFilterActivator.activate();
        return expenditureRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_expenditures_by_batch",
          description = "List expenditures for a specific batch.")
    @Transactional(readOnly = true)
    public List<Expenditure> listExpendituresByBatch(String batchId) {
        String tenantId = tenantFilterActivator.activate();
        return expenditureRepository.findByTenantIdAndBatchId(tenantId, batchId);
    }

    @Tool(name = "create_expenditure",
          description = "Record an expenditure. Provide batchId (optional), date (YYYY-MM-DD), category, amount, and description.")
    @Transactional
    public Expenditure createExpenditure(String batchId, String date,
                                          String category, String macroInvestment, String subType,
                                          Double amount, String description) {
        String tenantId = tenantFilterActivator.activate();
        Expenditure exp = Expenditure.builder()
            .tenantId(tenantId)
            .batchId(batchId)
            .date(LocalDate.parse(date))
            .category(category)
            .macroInvestment(macroInvestment)
            .subType(subType)
            .amount(amount)
            .description(description)
            .build();
        return expenditureRepository.save(exp);
    }

    @Tool(name = "get_total_expenditure_by_batch",
          description = "Calculate the total expenditure amount for a specific batch.")
    @Transactional(readOnly = true)
    public double getTotalExpenditureByBatch(String batchId) {
        String tenantId = tenantFilterActivator.activate();
        return expenditureRepository.findByTenantIdAndBatchId(tenantId, batchId)
            .stream()
            .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0)
            .sum();
    }
}
