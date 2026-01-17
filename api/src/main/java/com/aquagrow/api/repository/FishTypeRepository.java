package com.aquagrow.api.repository;

import com.aquagrow.api.model.FishType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FishTypeRepository extends JpaRepository<FishType, String> {
    List<FishType> findByTenantId(String tenantId);

    java.util.Optional<FishType> findByName(String name);
}
