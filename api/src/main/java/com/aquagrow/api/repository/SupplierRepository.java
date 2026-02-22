package com.aquagrow.api.repository;

import com.aquagrow.api.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {
    List<Supplier> findByTenantId(String tenantId);

    List<Supplier> findByTenantIdAndFeedTypesContaining(String tenantId, String feedType);

    java.util.Optional<Supplier> findByExternalId(String externalId);

    java.util.Optional<Supplier> findByNameAndTenantId(String name, String tenantId);
}
