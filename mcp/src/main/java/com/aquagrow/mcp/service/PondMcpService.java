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
 * Each tool requires a tenantId parameter for multi-tenant isolation.
 */
@Service
@RequiredArgsConstructor
public class PondMcpService {

    private final PondRepository pondRepository;
    private final TenantFilterActivator tenantFilterActivator;

    @Tool(name = "list_ponds",
          description = "List all ponds for a given tenant. Returns pond details including area, depth, type, status, and GPS coordinates.")
    @Transactional(readOnly = true)
    public List<Pond> listPonds(String tenantId) {
        tenantFilterActivator.activateFor(tenantId);
        return pondRepository.findByTenantId(tenantId);
    }

    @Tool(name = "get_pond",
          description = "Get a specific pond by its ID for a given tenant.")
    @Transactional(readOnly = true)
    public Pond getPond(String tenantId, String pondId) {
        tenantFilterActivator.activateFor(tenantId);
        return pondRepository.findById(pondId)
            .filter(p -> tenantId.equals(p.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
    }

    @Tool(name = "list_active_ponds",
          description = "List all ponds with status 'Active' for a given tenant.")
    @Transactional(readOnly = true)
    public List<Pond> listActivePonds(String tenantId) {
        tenantFilterActivator.activateFor(tenantId);
        return pondRepository.findByTenantIdAndStatus(tenantId, "Active");
    }

    @Tool(name = "create_pond",
          description = "Create a new pond for a tenant. Required: name, status (Active/Maintenance/Inactive). Optional: area (m2), depth (m), type, latitude, longitude.")
    @Transactional
    public Pond createPond(String tenantId, String name, String status, Double area,
                           Double depth, String type, Double latitude, Double longitude) {
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
    public Pond updatePondStatus(String tenantId, String pondId, String status) {
        tenantFilterActivator.activateFor(tenantId);
        Pond pond = pondRepository.findById(pondId)
            .filter(p -> tenantId.equals(p.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
        pond.setStatus(status);
        return pondRepository.save(pond);
    }

    @Tool(name = "delete_pond",
          description = "Delete a pond by ID. Use with caution - this is irreversible.")
    @Transactional
    public String deletePond(String tenantId, String pondId) {
        tenantFilterActivator.activateFor(tenantId);
        Pond pond = pondRepository.findById(pondId)
            .filter(p -> tenantId.equals(p.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
        pondRepository.delete(pond);
        return "Pond " + pondId + " deleted successfully";
    }
}
