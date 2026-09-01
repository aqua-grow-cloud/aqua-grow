package com.aquagrow.api.repository;

import com.aquagrow.api.model.FeedStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedStockRepository extends JpaRepository<FeedStock, String> {
    List<FeedStock> findByTenantId(String tenantId);

    @Query("SELECT f FROM FeedStock f WHERE f.tenantId = ?1 AND f.currentStock < f.threshold")
    List<FeedStock> findLowStockByTenantId(String tenantId);

    java.util.Optional<FeedStock> findByTenantIdAndFeedType(String tenantId, String feedType);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE FeedStock f SET f.currentStock = f.currentStock - :amount, f.lastUpdated = :now " +
           "WHERE f.tenantId = :tenantId AND f.feedType = :feedType AND f.currentStock >= :amount")
    int deductStock(
        @Param("tenantId") String tenantId,
        @Param("feedType") String feedType,
        @Param("amount") double amount,
        @Param("now") LocalDateTime now
    );
}
