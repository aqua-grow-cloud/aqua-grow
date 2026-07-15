package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.Pond;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PondRepository extends JpaRepository<Pond, String> {
    List<Pond> findByTenantId(String tenantId);
    List<Pond> findByTenantIdAndStatus(String tenantId, String status);
}
