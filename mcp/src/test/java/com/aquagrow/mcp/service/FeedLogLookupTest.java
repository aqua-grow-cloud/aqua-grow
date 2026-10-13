package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.repository.FeedLogRepository;
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
class FeedLogLookupTest {

    @Mock FeedLogRepository feedLogRepository;
    @Mock TenantFilterActivator tenantFilterActivator;
    @InjectMocks FeedLogLookup lookup;

    private static FeedLog log(String date, String type, Double kg) {
        return FeedLog.builder().batchId("b-3").feedType(type).amount(kg)
                .date(date == null ? null : LocalDate.parse(date)).tenantId("t1").build();
    }

    @Test
    void returnsOnlyLogsInsideTheInclusiveRange_oldestFirst_forTheCallersTenant() {
        when(tenantFilterActivator.activate()).thenReturn("t1");
        when(feedLogRepository.findByTenantIdAndBatchId("t1", "b-3")).thenReturn(List.of(
                log("2026-10-12", "Starter", 50.0), log("2026-09-30", "Starter", 1.0),
                log("2026-10-01", "King", 20.0), log("2026-10-13", "King", 2.0), log(null, "King", 3.0)));

        var conflicts = lookup.conflicts("b-3", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12));

        assertThat(conflicts).extracting("date").containsExactly("2026-10-01", "2026-10-12");
        assertThat(conflicts.get(0).feedType()).isEqualTo("King");
        assertThat(conflicts.get(0).amountKg()).isEqualTo(20.0);
        verify(tenantFilterActivator).activate();
        verify(feedLogRepository).findByTenantIdAndBatchId("t1", "b-3");
        verifyNoMoreInteractions(feedLogRepository);
    }
}
