package com.aquagrow.mcp.repository;

import com.aquagrow.mcp.model.FishSeed;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FishSeedRepository extends JpaRepository<FishSeed, String> {
    List<FishSeed> findByTenantId(String tenantId);
    List<FishSeed> findByTenantIdAndPondId(String tenantId, String pondId);
}
