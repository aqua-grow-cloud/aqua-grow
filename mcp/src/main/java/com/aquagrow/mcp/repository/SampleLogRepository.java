package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.SampleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SampleLogRepository extends JpaRepository<SampleLog, String> {
    List<SampleLog> findByTenantId(String tenantId);
    List<SampleLog> findByTenantIdAndBatchId(String tenantId, String batchId);
}
