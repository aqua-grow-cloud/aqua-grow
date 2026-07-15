package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.FishType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FishTypeRepository extends JpaRepository<FishType, String> {
    List<FishType> findByTenantId(String tenantId);
}
