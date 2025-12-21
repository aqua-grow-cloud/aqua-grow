package com.aquagrow.api.repository;

import com.aquagrow.api.model.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, String> {
    List<SalaryPayment> findByEmployeeId(String employeeId);
}
