package com.aquagrow.api.repository;

import com.aquagrow.api.model.Pond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PondRepository extends JpaRepository<Pond, String> {
    java.util.Optional<Pond> findByName(String name);
}
