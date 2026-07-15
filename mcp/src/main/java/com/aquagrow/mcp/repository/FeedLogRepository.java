package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.FeedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedLogRepository extends JpaRepository<FeedLog, String> {
    List<FeedLog> findByTenantId(String tenantId);
    List<FeedLog> findByTenantIdAndBatchId(String tenantId, String batchId);
}
