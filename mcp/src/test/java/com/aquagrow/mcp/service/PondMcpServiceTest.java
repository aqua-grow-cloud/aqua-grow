package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.Pond;
import com.aquagrow.mcp.repository.PondRepository;
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
class PondMcpServiceTest {

    @Mock
    private PondRepository pondRepository;

    @Mock
    private TenantFilterActivator tenantFilterActivator;

    @InjectMocks
    private PondMcpService pondMcpService;

    @Test
    void listPonds_usesTenantFromActivator_notAParameter() {
        when(tenantFilterActivator.activate()).thenReturn("tenant-a");
        Pond pond = Pond.builder().id("p1").tenantId("tenant-a").name("Pond 1").status("Active").build();
        when(pondRepository.findByTenantId("tenant-a")).thenReturn(List.of(pond));

        List<Pond> result = pondMcpService.listPonds();

        assertThat(result).containsExactly(pond);
        verify(pondRepository).findByTenantId("tenant-a");
    }

    @Test
    void getPond_belongingToDifferentTenant_isNotReturned() {
        when(tenantFilterActivator.activate()).thenReturn("tenant-a");
        Pond otherTenantsPond = Pond.builder().id("p1").tenantId("tenant-b").name("Pond 1").status("Active").build();
        when(pondRepository.findById("p1")).thenReturn(Optional.of(otherTenantsPond));

        assertThatThrownBy(() -> pondMcpService.getPond("p1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void createPond_assignsTenantIdFromContext() {
        when(tenantFilterActivator.activate()).thenReturn("tenant-a");
        when(pondRepository.save(any(Pond.class))).thenAnswer(inv -> inv.getArgument(0));

        Pond created = pondMcpService.createPond("New Pond", "Active", 100.0, 2.0, "Freshwater", null, null);

        assertThat(created.getTenantId()).isEqualTo("tenant-a");
    }
}
