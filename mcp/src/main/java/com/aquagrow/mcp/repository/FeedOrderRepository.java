package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.FeedOrder;
import com.aquagrow.mcp.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedOrderRepository extends JpaRepository<FeedOrder, String> {
    List<FeedOrder> findByTenantId(String tenantId);
    List<FeedOrder> findByTenantIdAndStatus(String tenantId, OrderStatus status);
}
