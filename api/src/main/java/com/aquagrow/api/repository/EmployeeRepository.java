package com.aquagrow.api.repository;

import com.aquagrow.api.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Employee e WHERE e.name = :name AND e.tenantId = :tenantId")
    java.util.Optional<Employee> findFirstByNameAndTenantId(String name, String tenantId);
}
