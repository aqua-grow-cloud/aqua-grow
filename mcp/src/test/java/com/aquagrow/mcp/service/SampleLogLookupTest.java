package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.SampleLog;
import com.aquagrow.mcp.repository.SampleLogRepository;
import com.aquagrow.mcp.service.SampleLogLookup.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SampleLogLookupTest {

    @Mock SampleLogRepository sampleLogRepository;
    @Mock TenantFilterActivator tenantFilterActivator;
    @InjectMocks SampleLogLookup lookup;

    private static SampleLog sample(String date, String fishTypeId, Integer count, Double abw) {
        return SampleLog.builder().batchId("b-1").fishTypeId(fishTypeId).date(LocalDate.parse(date))
                .sampleCount(count).averageWeight(abw).tenantId("t1").build();
    }

    @Test
    void findsSameDaySamplesOfTheSameFishTypeAndTheLatestEarlierOne() {
        when(tenantFilterActivator.activate()).thenReturn("t1");
        when(sampleLogRepository.findByTenantIdAndBatchId("t1", "b-1")).thenReturn(List.of(
                sample("2026-10-12", "ft-1", 20, 50.0),      // same day, same type -> duplicate
                sample("2026-10-12", "ft-2", 10, 80.0),      // same day, other type -> ignored
                sample("2026-09-20", "ft-1", 20, 30.0),      // earlier
                sample("2026-10-01", "ft-1", 20, 40.0),      // latest earlier
                sample("2026-10-01", "ft-2", 20, 99.0),      // other type
                sample("2026-10-20", "ft-1", 20, 70.0),      // later -> ignored
                sample("2026-09-30", "ft-1", 20, null)));    // no weight -> ignored as previous

        Context ctx = lookup.context("b-1", "ft-1", LocalDate.of(2026, 10, 12));

        assertThat(ctx.sameDay()).hasSize(1);
        assertThat(ctx.sameDay().get(0).sampleCount()).isEqualTo(20);
        assertThat(ctx.previous()).isNotNull();
        assertThat(ctx.previous().date()).isEqualTo("2026-10-01");
        assertThat(ctx.previous().averageWeight()).isEqualTo(40.0);
        verify(sampleLogRepository).findByTenantIdAndBatchId("t1", "b-1");
        verifyNoMoreInteractions(sampleLogRepository);
    }

    @Test
    void noHistory_givesEmptyContext() {
        when(tenantFilterActivator.activate()).thenReturn("t1");
        when(sampleLogRepository.findByTenantIdAndBatchId("t1", "b-1")).thenReturn(List.of());

        Context ctx = lookup.context("b-1", "ft-1", LocalDate.of(2026, 10, 12));

        assertThat(ctx.sameDay()).isEmpty();
        assertThat(ctx.previous()).isNull();
    }
}
