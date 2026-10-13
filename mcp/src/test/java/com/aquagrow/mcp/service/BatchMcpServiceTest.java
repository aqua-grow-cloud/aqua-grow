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
class BatchMcpServiceTest {

    @Mock FarmDataClient farmData;
    @Mock ResolverService resolver;
    private BatchMcpService service;

    @BeforeEach
    void setUp() {
        service = new BatchMcpService(farmData, resolver);
    }

    @Test
    void listBatches_returnsWhatTheApiReturns() {
        Batch batch = Batch.builder().id("b-1").name("Sept").stockingDate(LocalDate.of(2026, 9, 1)).build();
        when(farmData.batches()).thenReturn(List.of(batch));

        assertThat(service.listBatches()).containsExactly(batch);
    }

    @Test
    void getBatch_acceptsAnIdNameOrPondName_andReturnsTheBatchWithItsSeeds() {
        Batch batch = Batch.builder().id("b-1").name("Sept").fishSeeds(List.of(FishSeed.builder().pondId("p-1").quantity(10).build())).build();
        when(resolver.resolveAnyBatch("pond 3")).thenReturn(new ResolvedBatch("b-1", "Sept", List.of("Pond 3"), null, "Active"));
        when(farmData.batch("b-1")).thenReturn(batch);

        assertThat(service.getBatch("pond 3")).isSameAs(batch);
    }
}
