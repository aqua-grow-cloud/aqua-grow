package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FishType;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.repository.FishTypeRepository;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FishTypeService {

    private final FishTypeRepository fishTypeRepository;
    private final BatchRepository batchRepository;

    /**
     * Create a new fish type
     */
    @Transactional
    public FishType createFishType(FishType fishType) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        validateFishType(fishType);

        // Check for duplicate name
        Optional<FishType> existing = fishTypeRepository.findByName(fishType.getName());
        if (existing.isPresent() && existing.get().getTenantId().equals(tenantId)) {
            throw new BusinessException("Fish type with name '" + fishType.getName() + "' already exists");
        }

        fishType.setTenantId(tenantId);
        FishType saved = fishTypeRepository.save(fishType);

        log.info("Created fish type: {} ({})", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Get all fish types for current tenant
     */
    @Transactional(readOnly = true)
    public List<FishType> getAllFishTypes() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        return fishTypeRepository.findByTenantId(tenantId);
    }

    /**
     * Get fish type by ID
     */
    @Transactional(readOnly = true)
    public Optional<FishType> getFishTypeById(String id) {
        return fishTypeRepository.findById(id);
    }

    /**
     * Get fish type by name
     */
    @Transactional(readOnly = true)
    public Optional<FishType> getFishTypeByName(String name) {
        return fishTypeRepository.findByName(name);
    }

    /**
     * Update fish type
     */
    @Transactional
    public FishType updateFishType(String id, FishType updates) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        FishType existing = fishTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Fish type not found: " + id));

        // Verify tenant ownership
        if (!existing.getTenantId().equals(tenantId)) {
            throw new BusinessException("Fish type not found: " + id);
        }

        // Update fields with validation
        if (updates.getName() != null && !updates.getName().isEmpty()) {
            // Check for duplicate name (excluding current entity)
            Optional<FishType> duplicate = fishTypeRepository.findByName(updates.getName());
            if (duplicate.isPresent() && 
                !duplicate.get().getId().equals(id) && 
                duplicate.get().getTenantId().equals(tenantId)) {
                throw new BusinessException("Fish type with name '" + updates.getName() + "' already exists");
            }
            existing.setName(updates.getName());
        }

        if (updates.getScientificName() != null) {
            existing.setScientificName(updates.getScientificName());
        }

        FishType saved = fishTypeRepository.save(existing);
        log.info("Updated fish type: {}", id);
        return saved;
    }

    /**
     * Delete fish type
     */
    @Transactional
    public void deleteFishType(String id) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        FishType fishType = fishTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Fish type not found: " + id));

        // Verify tenant ownership
        if (!fishType.getTenantId().equals(tenantId)) {
            throw new BusinessException("Fish type not found: " + id);
        }

        // Check if fish type is in use
        if (isInUse(id)) {
            throw new BusinessException("Cannot delete fish type that is in use by batches");
        }

        fishTypeRepository.deleteById(id);
        log.info("Deleted fish type: {}", id);
    }

    /**
     * Check if fish type is being used in any batches
     */
    @Transactional(readOnly = true)
    public boolean isInUse(String fishTypeId) {
        List<Batch> batches = batchRepository.findAll();
        return batches.stream()
                .flatMap(batch -> batch.getFishSeeds().stream())
                .anyMatch(seed -> seed.getFishTypeId().equals(fishTypeId));
    }

    /**
     * Get count of batches using this fish type
     */
    @Transactional(readOnly = true)
    public long getBatchCountForFishType(String fishTypeId) {
        List<Batch> batches = batchRepository.findAll();
        return batches.stream()
                .filter(batch -> batch.getFishSeeds().stream()
                        .anyMatch(seed -> seed.getFishTypeId().equals(fishTypeId)))
                .count();
    }

    /**
     * Get all batches using this fish type
     */
    @Transactional(readOnly = true)
    public List<Batch> getBatchesForFishType(String fishTypeId) {
        List<Batch> batches = batchRepository.findAll();
        return batches.stream()
                .filter(batch -> batch.getFishSeeds().stream()
                        .anyMatch(seed -> seed.getFishTypeId().equals(fishTypeId)))
                .toList();
    }

    /**
     * Search fish types by name (case-insensitive, partial match)
     */
    @Transactional(readOnly = true)
    public List<FishType> searchByName(String query) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        return fishTypeRepository.findByTenantId(tenantId).stream()
                .filter(ft -> ft.getName().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    // ==================== Private Helper Methods ====================

    private void validateFishType(FishType fishType) {
        if (fishType.getName() == null || fishType.getName().trim().isEmpty()) {
            throw new BusinessException("Fish type name is required");
        }

        if (fishType.getName().length() > 100) {
            throw new BusinessException("Fish type name must not exceed 100 characters");
        }

        if (fishType.getScientificName() != null && fishType.getScientificName().length() > 200) {
            throw new BusinessException("Scientific name must not exceed 200 characters");
        }
    }
}
