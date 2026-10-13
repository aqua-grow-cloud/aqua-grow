package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.FishType;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.Supplier;
import com.aquagrow.mcp.repository.FishTypeRepository;
import com.aquagrow.mcp.repository.FishSeedRepository;
import com.aquagrow.mcp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP Tools for catalog data: fish types, fish seeds, suppliers.
 * Tenant scoping is derived from the authenticated caller's JWT
 * (see TenantFilter/TenantContext) — never from an LLM-supplied parameter.
 */
@Service
@RequiredArgsConstructor
public class CatalogMcpService {

    private final FishTypeRepository fishTypeRepository;
    private final FishSeedRepository fishSeedRepository;
    private final SupplierRepository supplierRepository;
    private final TenantFilterActivator tenantFilterActivator;

    // ---- Fish Types ----

    @Tool(name = "list_fish_types",
          description = "List all fish types configured for the current tenant (e.g. Tilapia, Shrimp, Catfish).")
    @Transactional(readOnly = true)
    public List<FishType> listFishTypes() {
        String tenantId = tenantFilterActivator.activate();
        return fishTypeRepository.findByTenantId(tenantId);
    }

    @Tool(name = "create_fish_type",
          description = "Register a new fish type. Provide name, optional scientificName, and cultivationType (FISH, SHRIMP, OTHER).")
    @Transactional
    public FishType createFishType(String name, String scientificName,
                                    String cultivationType) {
        String tenantId = tenantFilterActivator.activate();
        FishType fishType = FishType.builder()
            .tenantId(tenantId)
            .name(name)
            .scientificName(scientificName)
            .cultivationType(cultivationType != null
                ? com.aquagrow.mcp.model.CultivationType.valueOf(cultivationType.toUpperCase())
                : null)
            .build();
        return fishTypeRepository.save(fishType);
    }

    // ---- Fish Seeds ----

    @Tool(name = "list_fish_seeds",
          description = "List all fish seed records for the current tenant.")
    @Transactional(readOnly = true)
    public List<FishSeed> listFishSeeds() {
        String tenantId = tenantFilterActivator.activate();
        return fishSeedRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_fish_seeds_by_pond",
          description = "List fish seeds stocked in a specific pond.")
    @Transactional(readOnly = true)
    public List<FishSeed> listFishSeedsByPond(String pondId) {
        String tenantId = tenantFilterActivator.activate();
        return fishSeedRepository.findByTenantIdAndPondId(tenantId, pondId);
    }

    @Tool(name = "create_fish_seed",
          description = "Record fish seed stocking in a pond. Provide pondId, fishTypeId, date (YYYY-MM-DD), quantity, sizeInches or sizeGrams, unitPrice, and vendor.")
    @Transactional
    public FishSeed createFishSeed(String pondId, String fishTypeId,
                                    String date, Integer quantity, Double sizeInches,
                                    Double sizeGrams, Double unitPrice, String vendor) {
        String tenantId = tenantFilterActivator.activate();
        FishSeed seed = FishSeed.builder()
            .tenantId(tenantId)
            .pondId(pondId)
            .fishTypeId(fishTypeId)
            .date(LocalDate.parse(date))
            .quantity(quantity)
            .sizeInches(sizeInches)
            .sizeGrams(sizeGrams)
            .unitPrice(unitPrice)
            .vendor(vendor)
            .status("Active")
            .build();
        return fishSeedRepository.save(seed);
    }

    // ---- Suppliers ----

    @Tool(name = "list_suppliers",
          description = "List all suppliers for the current tenant.")
    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers() {
        String tenantId = tenantFilterActivator.activate();
        return supplierRepository.findByTenantId(tenantId);
    }

    @Tool(name = "create_supplier",
          description = "Create a new supplier. Provide name and optional phone, email, address, whatsappNumber.")
    @Transactional
    public Supplier createSupplier(String name, String phone,
                                    String email, String address, String whatsappNumber) {
        String tenantId = tenantFilterActivator.activate();
        Supplier supplier = Supplier.builder()
            .tenantId(tenantId)
            .name(name)
            .phone(phone)
            .email(email)
            .address(address)
            .whatsappNumber(whatsappNumber)
            .build();
        return supplierRepository.save(supplier);
    }
}
