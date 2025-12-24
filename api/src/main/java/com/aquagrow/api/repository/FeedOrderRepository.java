package com.aquagrow.api.repository;

import com.aquagrow.api.model.FeedOrder;
import com.aquagrow.api.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedOrderRepository extends JpaRepository<FeedOrder, String> {
    List<FeedOrder> findByTenantId(String tenantId);

    List<FeedOrder> findByTenantIdAndStatus(String tenantId, OrderStatus status);

    List<FeedOrder> findByTenantIdOrderByOrderDateDesc(String tenantId);
}
