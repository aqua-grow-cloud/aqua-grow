package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.Pond;
import com.aquagrow.mcp.repository.PondRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MCP Tools for Pond management.
 * Tenant scoping is derived from the authenticated caller's JWT
 * (see TenantFilter/TenantContext) — never from an LLM-supplied parameter.
 */
@Service
@RequiredArgsConstructor
public class PondMcpService {

    private final PondRepository pondRepository;
    private final TenantFilterActivator tenantFilterActivator;

    @Tool(name = "list_ponds",
          description = "List all ponds for the current tenant. Returns pond details including area, depth, type, status, and GPS coordinates.")
    @Transactional(readOnly = true)
    public List<Pond> listPonds() {
        String tenantId = tenantFilterActivator.activate();
        return pondRepository.findByTenantId(tenantId);
    }

    @Tool(name = "get_pond",
          description = "Get a specific pond by its ID for the current tenant.")
    @Transactional(readOnly = true)
    public Pond getPond(String pondId) {
        String tenantId = tenantFilterActivator.activate();
        return pondRepository.findById(pondId)
            .filter(p -> tenantId.equals(p.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
    }

    @Tool(name = "list_active_ponds",
          description = "List all ponds with status 'Active' for the current tenant.")
    @Transactional(readOnly = true)
    public List<Pond> listActivePonds() {
        String tenantId = tenantFilterActivator.activate();
        return pondRepository.findByTenantIdAndStatus(tenantId, "Active");
    }

    @Tool(name = "create_pond",
          description = "Create a new pond for the current tenant. Required: name, status (Active/Maintenance/Inactive). Optional: area (m2), depth (m), type, latitude, longitude.")
    @Transactional
    public Pond createPond(String name, String status, Double area,
                           Double depth, String type, Double latitude, Double longitude) {
        String tenantId = tenantFilterActivator.activate();
        Pond pond = Pond.builder()
            .tenantId(tenantId)
            .name(name)
            .status(status)
            .area(area)
            .depth(depth)
            .type(type)
            .latitude(latitude)
            .longitude(longitude)
            .build();
        return pondRepository.save(pond);
    }

    @Tool(name = "update_pond_status",
          description = "Update the status of a pond. Valid statuses: Active, Maintenance, Inactive.")
    @Transactional
    public Pond updatePondStatus(String pondId, String status) {
        String tenantId = tenantFilterActivator.activate();
        Pond pond = pondRepository.findById(pondId)
            .filter(p -> tenantId.equals(p.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
        pond.setStatus(status);
        return pondRepository.save(pond);
    }

    @Tool(name = "delete_pond",
          description = "Delete a pond by ID. Use with caution - this is irreversible.")
    @Transactional
    public String deletePond(String pondId) {
        String tenantId = tenantFilterActivator.activate();
        Pond pond = pondRepository.findById(pondId)
            .filter(p -> tenantId.equals(p.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
        pondRepository.delete(pond);
        return "Pond " + pondId + " deleted successfully";
    }
}
