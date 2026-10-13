package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP Tools for Batch management.
 * Tenant scoping is derived from the authenticated caller's JWT
 * (see TenantFilter/TenantContext) — never from an LLM-supplied parameter.
 */
@Service
@RequiredArgsConstructor
public class BatchMcpService {

    private final BatchRepository batchRepository;
    private final TenantFilterActivator tenantFilterActivator;

    @Tool(name = "list_batches",
          description = "List all batches for the current tenant. A batch represents a cultivation cycle with fish seeds stocked in ponds.")
    @Transactional(readOnly = true)
    public List<Batch> listBatches() {
        String tenantId = tenantFilterActivator.activate();
        return batchRepository.findByTenantId(tenantId);
    }

    @Tool(name = "get_batch",
          description = "Get a specific batch by ID including its fish seeds.")
    @Transactional(readOnly = true)
    public Batch getBatch(String batchId) {
        String tenantId = tenantFilterActivator.activate();
        return batchRepository.findById(batchId)
            .filter(b -> tenantId.equals(b.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
    }

    @Tool(name = "create_batch",
          description = "Create a new cultivation batch. Provide name, stockingDate (YYYY-MM-DD), and optional estimatedHarvestDate (YYYY-MM-DD).")
    @Transactional
    public Batch createBatch(String name, String stockingDate, String estimatedHarvestDate) {
        String tenantId = tenantFilterActivator.activate();
        Batch batch = Batch.builder()
            .tenantId(tenantId)
            .name(name)
            .stockingDate(LocalDate.parse(stockingDate))
            .estimatedHarvestDate(estimatedHarvestDate != null ? LocalDate.parse(estimatedHarvestDate) : null)
            .build();
        return batchRepository.save(batch);
    }
}
