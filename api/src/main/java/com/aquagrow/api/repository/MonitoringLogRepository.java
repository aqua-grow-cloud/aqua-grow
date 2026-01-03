package com.aquagrow.api.repository;

import com.aquagrow.api.model.MonitoringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoringLogRepository
        extends JpaRepository<MonitoringLog, String>, JpaSpecificationExecutor<MonitoringLog> {
    List<MonitoringLog> findByBatchId(String batchId);
}
