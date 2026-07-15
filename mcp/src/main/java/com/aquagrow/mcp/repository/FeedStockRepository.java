package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.FeedStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedStockRepository extends JpaRepository<FeedStock, String> {
    List<FeedStock> findByTenantId(String tenantId);
}
