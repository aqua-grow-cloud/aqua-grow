package com.aquagrow.api.repository;

import com.aquagrow.api.model.FeedStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedStockRepository extends JpaRepository<FeedStock, String> {
    List<FeedStock> findByTenantId(String tenantId);

    @Query("SELECT f FROM FeedStock f WHERE f.tenantId = ?1 AND f.currentStock < f.threshold")
    List<FeedStock> findLowStockByTenantId(String tenantId);

    java.util.Optional<FeedStock> findByTenantIdAndFeedType(String tenantId, String feedType);
}
