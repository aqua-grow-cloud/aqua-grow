package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Employee;
import com.aquagrow.api.model.SalaryPayment;
import com.aquagrow.api.repository.EmployeeRepository;
import com.aquagrow.api.repository.SalaryPaymentRepository;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryPaymentService {

    private final SalaryPaymentRepository salaryPaymentRepository;
    private final EmployeeRepository employeeRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Calculate net salary amount
     * Formula: baseSalary - advanceDeduction + bonus
     */
    public double calculateNetAmount(Double baseSalary, Double advanceDeduction, Double bonus) {
        double base = baseSalary != null ? baseSalary : 0.0;
        double advance = advanceDeduction != null ? advanceDeduction : 0.0;
        double bonusAmount = bonus != null ? bonus : 0.0;
        
        return base - advance + bonusAmount;
    }

    /**
     * Create a new salary payment for an employee
     * - Validates employee exists
     * - Prevents duplicate payments for same month
     * - Calculates net amount automatically
     * - Resets employee's advance to zero after payment
     */
    @Transactional
    public SalaryPayment createSalaryPayment(
            String employeeId,
            LocalDate paymentDate,
            Double baseSalary,
            Double advanceDeduction,
            Double bonus
    ) {
        log.info("Creating salary payment for employee: {}, month: {}", employeeId, formatMonth(paymentDate));

        // 1. Validate employee exists
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found with ID: " + employeeId));

        // 2. Check for duplicate payment
        String month = formatMonth(paymentDate);
        if (salaryPaymentRepository.existsByEmployeeIdAndMonth(employeeId, month)) {
            throw new BusinessException("Salary already paid for employee " + employeeId + " in month " + month);
        }

        // 3. Calculate net amount
        double netAmount = calculateNetAmount(baseSalary, advanceDeduction, bonus);

        // 4. Create payment
        SalaryPayment payment = SalaryPayment.builder()
                .employeeId(employeeId)
                .date(paymentDate)
                .baseSalary(baseSalary)
                .advanceDeduction(advanceDeduction)
                .bonus(bonus)
                .netAmount(netAmount)
                .month(month)
                .tenantId(TenantContext.getTenantId())
                .build();

        SalaryPayment savedPayment = salaryPaymentRepository.save(payment);

        // 5. Reset employee's advance to zero
        employee.setAdvanceTaken(0.0);
        employeeRepository.save(employee);

        log.info("Salary payment created successfully: {}", savedPayment.getId());
        return savedPayment;
    }

    /**
     * Get all salary payment history for an employee
     */
    public List<SalaryPayment> getEmployeeSalaryHistory(String employeeId) {
        log.debug("Fetching salary history for employee: {}", employeeId);
        return salaryPaymentRepository.findByEmployeeId(employeeId);
    }

    /**
     * Get all salary payments for a specific month
     * @param month Format: "YYYY-MM" (e.g., "2024-01")
     */
    public List<SalaryPayment> getPaymentsForMonth(String month) {
        log.debug("Fetching salary payments for month: {}", month);
        return salaryPaymentRepository.findByMonth(month);
    }

    /**
     * Format date to month string (YYYY-MM)
     */
    public String formatMonth(LocalDate date) {
        return date.format(MONTH_FORMATTER);
    }

    /**
     * Get all salary payments
     */
    public List<SalaryPayment> getAllPayments() {
        return salaryPaymentRepository.findAll();
    }

    /**
     * Get salary payment by ID
     */
    public SalaryPayment getPaymentById(String id) {
        return salaryPaymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Salary payment not found with ID: " + id));
    }

    /**
     * Delete salary payment
     * Note: This should be used carefully, mainly for correction purposes
     */
    @Transactional
    public void deletePayment(String id) {
        log.warn("Deleting salary payment: {}", id);
        SalaryPayment payment = getPaymentById(id);
        salaryPaymentRepository.delete(payment);
    }
}
