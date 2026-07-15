package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
    List<Supplier> findByTenantId(String tenantId);
}
