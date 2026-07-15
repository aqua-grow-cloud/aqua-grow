package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.MonitoringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MonitoringLogRepository extends JpaRepository<MonitoringLog, String> {
    List<MonitoringLog> findByTenantId(String tenantId);
    List<MonitoringLog> findByTenantIdAndBatchId(String tenantId, String batchId);
}
