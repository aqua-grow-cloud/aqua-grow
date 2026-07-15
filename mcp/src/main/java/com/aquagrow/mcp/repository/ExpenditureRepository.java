package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenditureRepository extends JpaRepository<Expenditure, String> {
    List<Expenditure> findByTenantId(String tenantId);
    List<Expenditure> findByTenantIdAndBatchId(String tenantId, String batchId);
    List<Expenditure> findByTenantIdAndCategory(String tenantId, String category);
}
