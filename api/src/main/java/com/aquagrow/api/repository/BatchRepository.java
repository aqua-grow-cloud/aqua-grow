package com.aquagrow.api.repository;

import com.aquagrow.api.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, String>, JpaSpecificationExecutor<Batch> {
    List<Batch> findDistinctByFishSeedsPondId(String pondId);

    java.util.Optional<Batch> findByName(String name);
}
