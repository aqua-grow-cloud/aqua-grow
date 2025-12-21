package com.aquagrow.api.repository;

import com.aquagrow.api.model.FishSeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FishSeedRepository extends JpaRepository<FishSeed, String> {
}
