package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.model.*;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogMcpServiceTest {

    @Mock FarmDataClient farmData;
    private CatalogMcpService service;

    @BeforeEach
    void setUp() {
        service = new CatalogMcpService(farmData);
    }

    @Test
    void fishTypesAndSuppliers_areReturnedAsTheApiGivesThem() {
        FishType type = FishType.builder().id("ft-1").name("Catfish").build();
        Supplier supplier = Supplier.builder().id("s-1").name("Agro Feeds").build();
        when(farmData.fishTypes()).thenReturn(List.of(type));
        when(farmData.suppliers()).thenReturn(List.of(supplier));

        assertThat(service.listFishTypes()).containsExactly(type);
        assertThat(service.listSuppliers()).containsExactly(supplier);
    }

    private void givenBatches() {
        Batch a = Batch.builder().id("b-1").name("Sept").fishSeeds(List.of(
                FishSeed.builder().id("sd-1").pondId("p-1").fishTypeId("ft-1").quantity(1000).sizeGrams(50.0).unitPrice(2.0).vendor("H").status("Active").build(),
                FishSeed.builder().id("sd-2").pondId("p-2").fishTypeId("ft-2").quantity(500).status("Harvested").build())).build();
        Batch b = Batch.builder().id("b-2").name("Oct").fishSeeds(List.of(
                FishSeed.builder().id("sd-3").pondId("p-1").fishTypeId("ft-1").quantity(200).status("Active").build())).build();
        when(farmData.batches()).thenReturn(List.of(a, b));
    }

    @Test
    void fishSeeds_areFlattenedFromBatches_withTheirBatchContext() {
        givenBatches();

        List<CatalogMcpService.SeedRow> seeds = service.listFishSeeds();

        assertThat(seeds).extracting(CatalogMcpService.SeedRow::id).containsExactly("sd-1", "sd-2", "sd-3");
        CatalogMcpService.SeedRow first = seeds.get(0);
        assertThat(first.batchId()).isEqualTo("b-1");
        assertThat(first.batchName()).isEqualTo("Sept");
        assertThat(first.pondId()).isEqualTo("p-1");
        assertThat(first.quantity()).isEqualTo(1000);
        assertThat(first.sizeGrams()).isEqualTo(50.0);
        assertThat(first.vendor()).isEqualTo("H");
        assertThat(first.status()).isEqualTo("Active");
    }

    @Test
    void fishSeedsByPond_filtersOnThePond() {
        givenBatches();

        assertThat(service.listFishSeedsByPond("p-1")).extracting(CatalogMcpService.SeedRow::id).containsExactly("sd-1", "sd-3");
        assertThat(service.listFishSeedsByPond("p-2")).extracting(CatalogMcpService.SeedRow::id).containsExactly("sd-2");
        assertThat(service.listFishSeedsByPond("nope")).isEmpty();
    }

    @Test
    void batchesWithoutSeeds_areFine() {
        when(farmData.batches()).thenReturn(List.of(Batch.builder().id("b-9").name("Empty").fishSeeds(List.of()).build(),
                Batch.builder().id("b-8").name("Null").fishSeeds(null).build()));

        assertThat(service.listFishSeeds()).isEmpty();
    }
}
