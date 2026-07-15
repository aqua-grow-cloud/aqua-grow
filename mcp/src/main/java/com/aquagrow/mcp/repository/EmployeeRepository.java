package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findByTenantId(String tenantId);
    List<Employee> findByTenantIdAndStatus(String tenantId, String status);
}
