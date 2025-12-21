package com.aquagrow.api.repository;

import com.aquagrow.api.model.FeedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedLogRepository extends JpaRepository<FeedLog, String> {
    List<FeedLog> findByBatchId(String batchId);
}
