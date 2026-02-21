package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FishStockEvent;
import com.aquagrow.api.model.FishStockEventType;
import com.aquagrow.api.repository.FishStockEventRepository;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing fish stock events.
 * Handles stock event creation, retrieval, and stock calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FishStockEventService {

    private final FishStockEventRepository fishStockEventRepository;

    /**
     * Creates a new fish stock event.
     * Sets the tenant ID from the current security context.
     *
     * @param event the fish stock event to create
     * @return the created fish stock event with generated ID
     * @throws BusinessException if tenant context is missing or event already exists
     */
    @Transactional
    public FishStockEvent createStockEvent(FishStockEvent event) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        // Check for duplicate events if referenceId is provided
        if (event.getReferenceId() != null && event.getType() != null) {
            boolean exists = fishStockEventRepository.existsByTypeAndReferenceId(
                    event.getType(), event.getReferenceId());
            if (exists) {
                throw new BusinessException(
                        String.format("Stock event already exists for type %s and reference %s",
                                event.getType(), event.getReferenceId()));
            }
        }

        event.setTenantId(tenantId);
        FishStockEvent saved = fishStockEventRepository.save(event);

        log.info("Created stock event: {} (type: {}, batch: {}, pond: {}, delta: {}, tenant: {})",
                saved.getId(), saved.getType(), saved.getBatchId(), saved.getPondId(),
                saved.getQuantityDelta(), tenantId);

        return saved;
    }

    /**
     * Retrieves all fish stock events.
     *
     * @return list of all fish stock events
     */
    @Transactional(readOnly = true)
    public List<FishStockEvent> getAllStockEvents() {
        List<FishStockEvent> events = fishStockEventRepository.findAll();

        log.debug("Retrieved {} stock events", events.size());

        return events;
    }

    /**
     * Retrieves all fish stock events for a specific batch.
     *
     * @param batchId the batch ID
     * @return list of stock events for the batch
     */
    @Transactional(readOnly = true)
    public List<FishStockEvent> getStockEventsByBatch(String batchId) {
        List<FishStockEvent> events = fishStockEventRepository.findByBatchId(batchId);

        log.debug("Found {} stock events for batch: {}", events.size(), batchId);

        return events;
    }

    /**
     * Retrieves all fish stock events for a specific pond.
     *
     * @param pondId the pond ID
     * @return list of stock events for the pond
     */
    @Transactional(readOnly = true)
    public List<FishStockEvent> getStockEventsByPond(String pondId) {
        List<FishStockEvent> events = fishStockEventRepository.findByPondId(pondId);

        log.debug("Found {} stock events for pond: {}", events.size(), pondId);

        return events;
    }

    /**
     * Calculates the current stock quantity for a batch.
     * Sums all quantity deltas (positive for SEED/TRANSFER_IN, negative for HARVEST/MORTALITY/TRANSFER_OUT).
     *
     * @param batchId the batch ID
     * @return the current stock quantity
     */
    @Transactional(readOnly = true)
    public Integer calculateCurrentStock(String batchId) {
        List<FishStockEvent> events = fishStockEventRepository.findByBatchId(batchId);

        int currentStock = events.stream()
                .mapToInt(FishStockEvent::getQuantityDelta)
                .sum();

        log.debug("Calculated current stock for batch {}: {}", batchId, currentStock);

        return currentStock;
    }

    /**
     * Calculates the current stock quantity for a pond.
     * Sums all quantity deltas for all batches in the pond.
     *
     * @param pondId the pond ID
     * @return the current stock quantity
     */
    @Transactional(readOnly = true)
    public Integer calculatePondStock(String pondId) {
        List<FishStockEvent> events = fishStockEventRepository.findByPondId(pondId);

        int currentStock = events.stream()
                .mapToInt(FishStockEvent::getQuantityDelta)
                .sum();

        log.debug("Calculated current stock for pond {}: {}", pondId, currentStock);

        return currentStock;
    }

    /**
     * Calculates stock breakdown by event type for a batch.
     *
     * @param batchId the batch ID
     * @return map of event type to total quantity
     */
    @Transactional(readOnly = true)
    public Map<FishStockEventType, Integer> calculateStockBreakdown(String batchId) {
        List<FishStockEvent> events = fishStockEventRepository.findByBatchId(batchId);

        Map<FishStockEventType, Integer> breakdown = events.stream()
                .collect(Collectors.groupingBy(
                        FishStockEvent::getType,
                        Collectors.summingInt(FishStockEvent::getQuantityDelta)
                ));

        log.debug("Calculated stock breakdown for batch {}: {}", batchId, breakdown);

        return breakdown;
    }

    /**
     * Calculates the estimated total biomass for a batch.
     * Uses the average weight from the most recent event with weight data.
     *
     * @param batchId the batch ID
     * @return the estimated biomass in grams, or null if no weight data available
     */
    @Transactional(readOnly = true)
    public Double calculateEstimatedBiomass(String batchId) {
        List<FishStockEvent> events = fishStockEventRepository.findByBatchId(batchId);

        // Get current stock
        int currentStock = events.stream()
                .mapToInt(FishStockEvent::getQuantityDelta)
                .sum();

        if (currentStock <= 0) {
            log.debug("No current stock for batch {}", batchId);
            return 0.0;
        }

        // Find the most recent event with weight data
        Double avgWeight = events.stream()
                .filter(e -> e.getAvgWeightGram() != null)
                .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
                .findFirst()
                .map(FishStockEvent::getAvgWeightGram)
                .orElse(null);

        if (avgWeight == null) {
            log.debug("No weight data available for batch {}", batchId);
            return null;
        }

        double biomass = currentStock * avgWeight;

        log.debug("Calculated estimated biomass for batch {}: {} grams ({} fish x {} g)",
                batchId, biomass, currentStock, avgWeight);

        return biomass;
    }

    /**
     * Checks if a stock event already exists for a given type and reference ID.
     *
     * @param type the event type
     * @param referenceId the reference ID (e.g., harvest ID, sampling ID)
     * @return true if event exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean stockEventExists(FishStockEventType type, String referenceId) {
        boolean exists = fishStockEventRepository.existsByTypeAndReferenceId(type, referenceId);

        log.debug("Stock event exists check (type: {}, referenceId: {}): {}",
                type, referenceId, exists);

        return exists;
    }
}
