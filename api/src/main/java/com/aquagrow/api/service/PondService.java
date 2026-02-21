package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Pond;
import com.aquagrow.api.repository.PondRepository;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing ponds and pond analytics.
 * Handles pond lifecycle, capacity calculations, and stocking density validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PondService {

    private final PondRepository pondRepository;

    // Standard stocking density (fish per m²) for different pond types
    private static final double DEFAULT_STOCKING_DENSITY = 10.0; // fish/m²
    private static final double INTENSIVE_STOCKING_DENSITY = 20.0; // fish/m²
    private static final double EXTENSIVE_STOCKING_DENSITY = 5.0; // fish/m²

    /**
     * Creates a new pond.
     * Sets the tenant ID from the current security context.
     *
     * @param pond the pond to create
     * @return the created pond with generated ID
     * @throws BusinessException if tenant context is missing or validation fails
     */
    @Transactional
    public Pond createPond(Pond pond) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        validatePond(pond);

        pond.setTenantId(tenantId);
        Pond saved = pondRepository.save(pond);

        log.info("Created pond: {} (name: {}, area: {} m², tenant: {})",
                saved.getId(), saved.getName(), saved.getArea(), tenantId);

        return saved;
    }

    /**
     * Retrieves all ponds.
     *
     * @return list of all ponds
     */
    @Transactional(readOnly = true)
    public List<Pond> getAllPonds() {
        List<Pond> ponds = pondRepository.findAll();

        log.debug("Retrieved {} ponds", ponds.size());

        return ponds;
    }

    /**
     * Retrieves a pond by ID.
     *
     * @param id the pond ID
     * @return the pond if found
     * @throws BusinessException if pond not found
     */
    @Transactional(readOnly = true)
    public Pond getPondById(String id) {
        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pond not found with id: " + id));

        log.debug("Retrieved pond: {}", id);

        return pond;
    }

    /**
     * Retrieves a pond by ID as Optional.
     *
     * @param id the pond ID
     * @return Optional containing the pond if found
     */
    @Transactional(readOnly = true)
    public Optional<Pond> findPondById(String id) {
        return pondRepository.findById(id);
    }

    /**
     * Updates an existing pond.
     *
     * @param id the pond ID
     * @param pondDetails the updated pond details
     * @return the updated pond
     * @throws BusinessException if pond not found or validation fails
     */
    @Transactional
    public Pond updatePond(String id, Pond pondDetails) {
        Pond pond = getPondById(id);

        validatePond(pondDetails);

        pond.setName(pondDetails.getName());
        pond.setArea(pondDetails.getArea());
        pond.setDepth(pondDetails.getDepth());
        pond.setType(pondDetails.getType());
        pond.setStatus(pondDetails.getStatus());
        pond.setCurrentBatchId(pondDetails.getCurrentBatchId());
        pond.setLatitude(pondDetails.getLatitude());
        pond.setLongitude(pondDetails.getLongitude());

        Pond updated = pondRepository.save(pond);

        log.info("Updated pond: {} (name: {}, area: {} m²)", id, updated.getName(), updated.getArea());

        return updated;
    }

    /**
     * Deletes a pond.
     *
     * @param id the pond ID
     * @throws BusinessException if pond not found or pond has active batch
     */
    @Transactional
    public void deletePond(String id) {
        Pond pond = getPondById(id);

        // Prevent deletion if pond has an active batch
        if (pond.getCurrentBatchId() != null && !pond.getCurrentBatchId().isEmpty()) {
            throw new BusinessException(
                    String.format("Cannot delete pond %s: it has an active batch %s",
                            pond.getName(), pond.getCurrentBatchId()));
        }

        pondRepository.delete(pond);

        log.info("Deleted pond: {} (name: {})", id, pond.getName());
    }

    /**
     * Calculates the water volume of a pond.
     * Volume = Area × Depth
     *
     * @param pondId the pond ID
     * @return water volume in cubic meters (m³), or null if area or depth is missing
     */
    @Transactional(readOnly = true)
    public Double calculateWaterVolume(String pondId) {
        Pond pond = getPondById(pondId);

        if (pond.getArea() == null || pond.getDepth() == null) {
            log.warn("Cannot calculate volume for pond {}: missing area or depth", pondId);
            return null;
        }

        double volume = pond.getArea() * pond.getDepth();

        log.debug("Calculated water volume for pond {}: {} m³", pondId, volume);

        return volume;
    }

    /**
     * Calculates the maximum recommended stocking capacity for a pond.
     * Based on pond type and area.
     *
     * @param pondId the pond ID
     * @return maximum stocking capacity (number of fish), or null if area is missing
     */
    @Transactional(readOnly = true)
    public Integer calculateMaxStockingCapacity(String pondId) {
        Pond pond = getPondById(pondId);

        if (pond.getArea() == null) {
            log.warn("Cannot calculate stocking capacity for pond {}: missing area", pondId);
            return null;
        }

        double density = getStockingDensityForType(pond.getType());
        int capacity = (int) (pond.getArea() * density);

        log.debug("Calculated max stocking capacity for pond {} (type: {}): {} fish ({} fish/m²)",
                pondId, pond.getType(), capacity, density);

        return capacity;
    }

    /**
     * Calculates the current stocking density for a pond.
     * Requires current stock count to be provided.
     *
     * @param pondId the pond ID
     * @param currentStock the current number of fish in the pond
     * @return stocking density in fish/m², or null if area is missing
     */
    @Transactional(readOnly = true)
    public Double calculateCurrentStockingDensity(String pondId, Integer currentStock) {
        Pond pond = getPondById(pondId);

        if (pond.getArea() == null) {
            log.warn("Cannot calculate stocking density for pond {}: missing area", pondId);
            return null;
        }

        if (currentStock == null || currentStock < 0) {
            return 0.0;
        }

        double density = currentStock / pond.getArea();

        log.debug("Calculated current stocking density for pond {}: {} fish/m² ({} fish in {} m²)",
                pondId, density, currentStock, pond.getArea());

        return density;
    }

    /**
     * Validates if a pond can accommodate additional fish.
     *
     * @param pondId the pond ID
     * @param currentStock the current number of fish in the pond
     * @param additionalFish the number of fish to add
     * @return true if pond can accommodate the additional fish, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean canAccommodateFish(String pondId, Integer currentStock, Integer additionalFish) {
        Integer maxCapacity = calculateMaxStockingCapacity(pondId);

        if (maxCapacity == null) {
            log.warn("Cannot validate stocking for pond {}: missing capacity data", pondId);
            return false;
        }

        int totalStock = (currentStock != null ? currentStock : 0) + (additionalFish != null ? additionalFish : 0);
        boolean canAccommodate = totalStock <= maxCapacity;

        log.debug("Pond {} stocking validation: current {}, additional {}, total {}, max {}, can accommodate: {}",
                pondId, currentStock, additionalFish, totalStock, maxCapacity, canAccommodate);

        return canAccommodate;
    }

    /**
     * Retrieves all ponds with a specific status.
     *
     * @param status the pond status (Active, Maintenance, Inactive)
     * @return list of ponds with the specified status
     */
    @Transactional(readOnly = true)
    public List<Pond> getPondsByStatus(String status) {
        List<Pond> ponds = pondRepository.findAll().stream()
                .filter(pond -> status.equalsIgnoreCase(pond.getStatus()))
                .collect(Collectors.toList());

        log.debug("Found {} ponds with status: {}", ponds.size(), status);

        return ponds;
    }

    /**
     * Retrieves all active ponds (status = "Active").
     *
     * @return list of active ponds
     */
    @Transactional(readOnly = true)
    public List<Pond> getActivePonds() {
        return getPondsByStatus("Active");
    }

    /**
     * Retrieves all available ponds (active ponds without a current batch).
     *
     * @return list of available ponds
     */
    @Transactional(readOnly = true)
    public List<Pond> getAvailablePonds() {
        List<Pond> ponds = pondRepository.findAll().stream()
                .filter(pond -> "Active".equalsIgnoreCase(pond.getStatus()))
                .filter(pond -> pond.getCurrentBatchId() == null || pond.getCurrentBatchId().isEmpty())
                .collect(Collectors.toList());

        log.debug("Found {} available ponds", ponds.size());

        return ponds;
    }

    /**
     * Calculates the total pond area across all ponds.
     *
     * @return total area in m²
     */
    @Transactional(readOnly = true)
    public Double calculateTotalPondArea() {
        double totalArea = pondRepository.findAll().stream()
                .filter(pond -> pond.getArea() != null)
                .mapToDouble(Pond::getArea)
                .sum();

        log.debug("Calculated total pond area: {} m²", totalArea);

        return totalArea;
    }

    /**
     * Calculates the utilization rate of ponds.
     * Utilization = (Occupied Ponds / Total Active Ponds) × 100
     *
     * @return utilization percentage, or 0 if no active ponds
     */
    @Transactional(readOnly = true)
    public Double calculatePondUtilization() {
        List<Pond> activePonds = getActivePonds();

        if (activePonds.isEmpty()) {
            return 0.0;
        }

        long occupiedCount = activePonds.stream()
                .filter(pond -> pond.getCurrentBatchId() != null && !pond.getCurrentBatchId().isEmpty())
                .count();

        double utilization = (occupiedCount * 100.0) / activePonds.size();

        log.debug("Calculated pond utilization: {}% ({} occupied / {} active)",
                utilization, occupiedCount, activePonds.size());

        return utilization;
    }

    /**
     * Validates pond data.
     *
     * @param pond the pond to validate
     * @throws BusinessException if validation fails
     */
    private void validatePond(Pond pond) {
        if (pond.getName() == null || pond.getName().trim().isEmpty()) {
            throw new BusinessException("Pond name is required");
        }

        if (pond.getStatus() == null || pond.getStatus().trim().isEmpty()) {
            throw new BusinessException("Pond status is required");
        }

        if (pond.getArea() != null && pond.getArea() <= 0) {
            throw new BusinessException("Pond area must be greater than 0");
        }

        if (pond.getDepth() != null && pond.getDepth() <= 0) {
            throw new BusinessException("Pond depth must be greater than 0");
        }
    }

    /**
     * Gets the stocking density for a pond type.
     *
     * @param type the pond type
     * @return stocking density in fish/m²
     */
    private double getStockingDensityForType(String type) {
        if (type == null) {
            return DEFAULT_STOCKING_DENSITY;
        }

        return switch (type.toLowerCase()) {
            case "intensive" -> INTENSIVE_STOCKING_DENSITY;
            case "extensive" -> EXTENSIVE_STOCKING_DENSITY;
            default -> DEFAULT_STOCKING_DENSITY;
        };
    }
}
