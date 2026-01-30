package com.aquagrow.api.repository;

import com.aquagrow.api.model.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, String> {
    
    /**
     * Find all salary payments for a specific employee
     */
    List<SalaryPayment> findByEmployeeId(String employeeId);
    
    /**
     * Find all salary payments for a specific month (format: YYYY-MM)
     */
    List<SalaryPayment> findByMonth(String month);
    
    /**
     * Find salary payment for a specific employee and month
     * Useful to check if salary was already paid for a given month
     */
    Optional<SalaryPayment> findByEmployeeIdAndMonth(String employeeId, String month);
    
    /**
     * Find all salary payments between two dates (inclusive)
     */
    List<SalaryPayment> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Check if salary payment exists for a specific employee and month
     * Prevents duplicate salary payments for the same month
     */
    boolean existsByEmployeeIdAndMonth(String employeeId, String month);
}
