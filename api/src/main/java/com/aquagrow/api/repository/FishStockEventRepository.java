package com.aquagrow.api.repository;

import com.aquagrow.api.model.FishStockEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FishStockEventRepository extends JpaRepository<FishStockEvent, String> {
    List<FishStockEvent> findByBatchId(String batchId);

    List<FishStockEvent> findByPondId(String pondId);

    boolean existsByTypeAndReferenceId(com.aquagrow.api.model.FishStockEventType type, String referenceId);
}
