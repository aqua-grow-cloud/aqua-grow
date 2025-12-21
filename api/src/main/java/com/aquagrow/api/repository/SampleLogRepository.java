package com.aquagrow.api.repository;

import com.aquagrow.api.model.SampleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SampleLogRepository extends JpaRepository<SampleLog, String> {
    List<SampleLog> findByBatchId(String batchId);
}
