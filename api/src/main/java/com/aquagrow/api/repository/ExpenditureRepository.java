package com.aquagrow.api.repository;

import com.aquagrow.api.model.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenditureRepository extends JpaRepository<Expenditure, String> {
    List<Expenditure> findByBatchId(String batchId);
}
