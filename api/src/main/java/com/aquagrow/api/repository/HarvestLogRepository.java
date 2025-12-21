package com.aquagrow.api.repository;

import com.aquagrow.api.model.HarvestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HarvestLogRepository extends JpaRepository<HarvestLog, String> {
    List<HarvestLog> findByBatchId(String batchId);
}
