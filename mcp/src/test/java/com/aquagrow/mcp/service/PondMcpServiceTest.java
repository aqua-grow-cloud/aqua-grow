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
class PondMcpServiceTest {

    @Mock FarmDataClient farmData;
    private PondMcpService service;

    @BeforeEach
    void setUp() {
        service = new PondMcpService(farmData);
    }

    @Test
    void listPonds_returnsWhatTheApiReturns() {
        Pond pond = Pond.builder().id("p-1").name("Pond 1").status("Active").build();
        when(farmData.ponds()).thenReturn(List.of(pond));

        assertThat(service.listPonds()).containsExactly(pond);
    }

    @Test
    void listActivePonds_usesTheActiveEndpoint() {
        Pond pond = Pond.builder().id("p-1").name("Pond 1").status("Active").build();
        when(farmData.activePonds()).thenReturn(List.of(pond));

        assertThat(service.listActivePonds()).containsExactly(pond);
        verify(farmData, never()).ponds();
    }

    @Test
    void getPond_returnsThePond_andPassesNotFoundThrough() {
        Pond pond = Pond.builder().id("p-1").name("Pond 1").build();
        when(farmData.pond("p-1")).thenReturn(pond);
        when(farmData.pond("nope")).thenThrow(new ToolUserException("Pond not found: nope"));

        assertThat(service.getPond("p-1")).isSameAs(pond);
        assertThatThrownBy(() -> service.getPond("nope")).isInstanceOf(ToolUserException.class).hasMessage("Pond not found: nope");
    }
}
