package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.FishType;
import com.aquagrow.mcp.model.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Read-only catalogue tools: fish types, the fish seeds stocked in batches, and suppliers. */
@Service
@RequiredArgsConstructor
public class CatalogMcpService {

    /** One stocking of fish seed, with the batch it belongs to. */
    public record SeedRow(String batchId, String batchName, String id, String pondId, String date, String fishTypeId,
                          Integer quantity, Double sizeInches, Double sizeGrams, Double unitPrice, String vendor,
                          String status) {
    }

    private final FarmDataClient farmData;

    @Tool(name = "list_fish_types",
          description = "List the fish and shrimp species set up for the farm (e.g. Tilapia, Shrimp, Catfish).")
    public List<FishType> listFishTypes() {
        return farmData.fishTypes();
    }

    @Tool(name = "list_fish_seeds",
          description = "List all fish seed stockings (pond, fish type, quantity, size, unit price, vendor, status), each with its batch.")
    public List<SeedRow> listFishSeeds() {
        return seeds(null);
    }

    @Tool(name = "list_fish_seeds_by_pond",
          description = "List the fish seed stockings of one pond, each with its batch.")
    public List<SeedRow> listFishSeedsByPond(@ToolParam(description = "The pond id (from list_ponds)") String pondId) {
        return seeds(pondId);
    }

    @Tool(name = "list_suppliers",
          description = "List the feed and seed suppliers with their contact details.")
    public List<Supplier> listSuppliers() {
        return farmData.suppliers();
    }

    /** Seeds live inside their batches in the API, so they are read from the batch list. */
    private List<SeedRow> seeds(String pondId) {
        List<SeedRow> rows = new ArrayList<>();
        for (Batch batch : farmData.batches()) {
            if (batch.getFishSeeds() == null) {
                continue;
            }
            for (FishSeed seed : batch.getFishSeeds()) {
                if (pondId != null && !pondId.equals(seed.getPondId())) {
                    continue;
                }
                rows.add(new SeedRow(batch.getId(), batch.getName(), seed.getId(), seed.getPondId(),
                        seed.getDate() == null ? null : seed.getDate().toString(), seed.getFishTypeId(),
                        seed.getQuantity(), seed.getSizeInches(), seed.getSizeGrams(), seed.getUnitPrice(),
                        seed.getVendor(), seed.getStatus()));
            }
        }
        return rows;
    }
}
