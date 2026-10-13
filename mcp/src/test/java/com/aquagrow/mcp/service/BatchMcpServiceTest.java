package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.repository.BatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchMcpServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private TenantFilterActivator tenantFilterActivator;

    @InjectMocks
    private BatchMcpService batchMcpService;

    @Test
    void listBatches_usesTenantFromActivator_notAParameter() {
        when(tenantFilterActivator.activate()).thenReturn("tenant-a");
        Batch batch = Batch.builder().id("b1").tenantId("tenant-a").name("Batch 1").build();
        when(batchRepository.findByTenantId("tenant-a")).thenReturn(List.of(batch));

        List<Batch> result = batchMcpService.listBatches();

        assertThat(result).containsExactly(batch);
        verify(batchRepository).findByTenantId("tenant-a");
    }

    @Test
    void getBatch_belongingToDifferentTenant_isNotReturned() {
        when(tenantFilterActivator.activate()).thenReturn("tenant-a");
        Batch otherTenantsBatch = Batch.builder().id("b1").tenantId("tenant-b").name("Batch 1").build();
        when(batchRepository.findById("b1")).thenReturn(Optional.of(otherTenantsBatch));

        assertThatThrownBy(() -> batchMcpService.getBatch("b1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void createBatch_assignsTenantIdFromContext() {
        when(tenantFilterActivator.activate()).thenReturn("tenant-a");
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        Batch created = batchMcpService.createBatch("New Batch", "2026-01-01", null);

        assertThat(created.getTenantId()).isEqualTo("tenant-a");
    }
}
