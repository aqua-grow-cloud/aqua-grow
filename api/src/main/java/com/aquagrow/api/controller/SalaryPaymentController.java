package com.aquagrow.api.controller;

import com.aquagrow.api.model.SalaryPayment;
import com.aquagrow.api.service.SalaryPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salary-payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SalaryPaymentController {

    private final SalaryPaymentService salaryPaymentService;

    /**
     * Get all salary payments
     */
    @GetMapping
    public List<SalaryPayment> getAllPayments() {
        log.debug("GET /api/salary-payments - Fetching all salary payments");
        return salaryPaymentService.getAllPayments();
    }

    /**
     * Get salary payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SalaryPayment> getPaymentById(@PathVariable String id) {
        log.debug("GET /api/salary-payments/{} - Fetching payment by ID", id);
        SalaryPayment payment = salaryPaymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    /**
     * Get salary payment history for a specific employee
     */
    @GetMapping("/employee/{employeeId}")
    public List<SalaryPayment> getEmployeeSalaryHistory(@PathVariable String employeeId) {
        log.debug("GET /api/salary-payments/employee/{} - Fetching salary history", employeeId);
        return salaryPaymentService.getEmployeeSalaryHistory(employeeId);
    }

    /**
     * Get all salary payments for a specific month
     * @param month Format: YYYY-MM (e.g., "2024-01")
     */
    @GetMapping("/month/{month}")
    public List<SalaryPayment> getPaymentsForMonth(@PathVariable String month) {
        log.debug("GET /api/salary-payments/month/{} - Fetching payments for month", month);
        return salaryPaymentService.getPaymentsForMonth(month);
    }

    /**
     * Create a new salary payment
     * Request body format:
     * {
     *   "employeeId": "emp-001",
     *   "date": "2024-01-31",
     *   "baseSalary": 5000.0,
     *   "advanceDeduction": 500.0,  // optional
     *   "bonus": 200.0              // optional
     * }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaryPayment createPayment(@RequestBody Map<String, Object> requestBody) {
        log.info("POST /api/salary-payments - Creating new salary payment");

        String employeeId = (String) requestBody.get("employeeId");
        LocalDate date = LocalDate.parse((String) requestBody.get("date"));
        Double baseSalary = getDoubleValue(requestBody, "baseSalary");
        Double advanceDeduction = getDoubleValue(requestBody, "advanceDeduction");
        Double bonus = getDoubleValue(requestBody, "bonus");

        return salaryPaymentService.createSalaryPayment(
                employeeId,
                date,
                baseSalary,
                advanceDeduction,
                bonus
        );
    }

    /**
     * Delete a salary payment
     * Note: Use with caution - mainly for corrections
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable String id) {
        log.warn("DELETE /api/salary-payments/{} - Deleting salary payment", id);
        salaryPaymentService.deletePayment(id);
    }

    /**
     * Helper method to safely extract Double values from request body
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        return null;
    }
}
