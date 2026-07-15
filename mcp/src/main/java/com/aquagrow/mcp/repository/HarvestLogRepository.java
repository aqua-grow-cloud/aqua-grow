package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.HarvestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HarvestLogRepository extends JpaRepository<HarvestLog, String> {
    List<HarvestLog> findByTenantId(String tenantId);
    List<HarvestLog> findByTenantIdAndBatchId(String tenantId, String batchId);
}
