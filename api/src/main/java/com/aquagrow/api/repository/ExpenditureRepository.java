package com.aquagrow.api.repository;

import com.aquagrow.api.model.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenditureRepository extends JpaRepository<Expenditure, String>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Expenditure> {
    List<Expenditure> findByBatchId(String batchId);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Expenditure e WHERE e.tenantId = :tenantId AND e.date BETWEEN :startDate AND :endDate")
    List<Expenditure> findByTenantIdAndDateBetween(String tenantId, java.time.LocalDate startDate,
            java.time.LocalDate endDate);
}
