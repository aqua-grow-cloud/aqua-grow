package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, String> {
    List<Batch> findByTenantId(String tenantId);
}
