package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.FishStockEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FishStockEventRepository extends JpaRepository<FishStockEvent, String> {
    List<FishStockEvent> findByTenantId(String tenantId);
    List<FishStockEvent> findByTenantIdAndBatchId(String tenantId, String batchId);
    List<FishStockEvent> findByTenantIdAndPondId(String tenantId, String pondId);
}
