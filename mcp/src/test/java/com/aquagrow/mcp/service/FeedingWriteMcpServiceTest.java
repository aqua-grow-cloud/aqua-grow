package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.AquaGrowApiClient;
import com.aquagrow.mcp.client.BulkFeedLogRequest;
import com.aquagrow.mcp.client.BulkFeedLogResponse;
import com.aquagrow.mcp.client.FeedStockView;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.config.FeedingProperties;
import com.aquagrow.mcp.confirm.PreviewTokenService;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.FeedingWriteMcpService.FeedingPreview;
import com.aquagrow.mcp.service.FeedingWriteMcpService.FeedingResult;
import com.aquagrow.mcp.service.ResolverService.AmbiguousReferenceException;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedingWriteMcpServiceTest {

    private static final String TENANT = "t1";
    private static final String FEED = "King Fish Feed";

    @Mock ResolverService resolver;
    @Mock AquaGrowApiClient apiClient;
    @Mock FeedLogLookup feedLogLookup;

    private PreviewTokenService tokens;
    private FeedingWriteMcpService service;

    @BeforeEach
    void setUp() {
        tokens = new PreviewTokenService(Duration.ofMinutes(10));
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        service = new FeedingWriteMcpService(resolver, apiClient, feedLogLookup,
                tokens, clock, new FeedingProperties(40.0, 5000.0, 62));
        TenantContext.setTenantId(TENANT);
        loginAs("user-1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private static void loginAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("tok-" + subject).header("alg", "none").subject(subject)
                .claim("tenant_id", TENANT).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void givenFarm(double stockKg) {
        when(resolver.resolveBatch("pond 3"))
                .thenReturn(new ResolvedBatch("b-3", "Oct Catfish", List.of("Pond 3"), LocalDate.of(2026, 9, 1)));
        when(resolver.resolveFeedType("king fish")).thenReturn(FEED);
        when(apiClient.getFeedStock()).thenReturn(List.of(new FeedStockView("s1", FEED, stockKg, 100.0, "kg")));
        givenExistingLogs();
    }

    private void givenExistingLogs(String... dates) {
        lenient().when(feedLogLookup.conflicts(eq("b-3"), any(), any())).thenAnswer(inv -> {
            LocalDate from = inv.getArgument(1);
            LocalDate to = inv.getArgument(2);
            return java.util.Arrays.stream(dates).map(LocalDate::parse)
                    .filter(d -> !d.isBefore(from) && !d.isAfter(to)).sorted()
                    .map(d -> new FeedingWriteMcpService.Conflict(d.toString(), FEED, 100.0)).toList();
        });
    }

    private FeedingPreview previewExample() {
        return service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-12", 11.0, null, null, null);
    }

    // ---- preview: the worked example ----

    @Test
    void preview_elevenBagsPerDayOf40kgFor12Days_is5280kg() {
        givenFarm(10000.0);

        FeedingPreview p = previewExample();

        assertThat(p.status()).isEqualTo("READY_TO_CONFIRM");
        assertThat(p.days()).isEqualTo(12);
        assertThat(p.kgPerDay()).isEqualTo(440.0);
        assertThat(p.bagsPerDay()).isEqualTo(11.0);
        assertThat(p.bagSizeKg()).isEqualTo(40.0);
        assertThat(p.totalKg()).isEqualTo(5280.0);
        assertThat(p.totalBags()).isEqualTo(132.0);
        assertThat(p.stockBeforeKg()).isEqualTo(10000.0);
        assertThat(p.stockAfterKg()).isEqualTo(4720.0);
        assertThat(p.rows()).hasSize(12);
        assertThat(p.rows().get(0).date()).isEqualTo("2026-10-01");
        assertThat(p.rows().get(11).date()).isEqualTo("2026-10-12");
        assertThat(p.rows()).allSatisfy(r -> assertThat(r.kg()).isEqualTo(440.0));
        assertThat(p.batchId()).isEqualTo("b-3");
        assertThat(p.batchName()).isEqualTo("Oct Catfish");
        assertThat(p.feedType()).isEqualTo(FEED);
        assertThat(p.previewToken()).isNotBlank();
        assertThat(p.message()).containsIgnoringCase("nothing has been saved").contains("confirm_feeding");
        verify(apiClient, never()).bulkFeed(any());
    }

    @Test
    void preview_kgPerDayPath() {
        givenFarm(10000.0);

        FeedingPreview p = service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-03", null, 100.0, null, null);

        assertThat(p.totalKg()).isEqualTo(300.0);
        assertThat(p.bagsPerDay()).isNull();
        assertThat(p.rows()).hasSize(3);
    }

    @Test
    void preview_bagSizeOverrideAndDefault() {
        givenFarm(10000.0);

        FeedingPreview custom = service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-01", 2.0, null, 25.0, null);
        assertThat(custom.kgPerDay()).isEqualTo(50.0);
        assertThat(custom.bagSizeKg()).isEqualTo(25.0);

        FeedingPreview dflt = service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-01", 2.0, null, null, null);
        assertThat(dflt.kgPerDay()).isEqualTo(80.0);
    }

    @Test
    void preview_singleDayAndTodayAreAllowed() {
        givenFarm(10000.0);

        FeedingPreview p = service.previewFeeding("pond 3", "king fish", "2026-10-13", "2026-10-13", 1.0, null, null, null);

        assertThat(p.days()).isEqualTo(1);
    }

    @Test
    void preview_fractionalBagsRoundToTwoDecimals() {
        givenFarm(10000.0);

        FeedingPreview p = service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-03", 0.333, null, 40.0, null);

        assertThat(p.kgPerDay()).isEqualTo(13.32);
        assertThat(p.totalKg()).isEqualTo(39.96);
    }

    // ---- preview: validation ----

    @Test
    void preview_requiresExactlyOneOfBagsOrKg() {
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-02", null, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("bagsPerDay or kgPerDay");
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-02", 1.0, 40.0, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("only one");
    }

    @Test
    void preview_rejectsNonPositiveAndExcessiveAmounts() {
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-02", 0.0, null, null, null))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-02", -1.0, null, null, null))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-02", null, 5000.01, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("5000");
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-02", 1.0, null, 0.0, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("bagSizeKg");
    }

    @Test
    void preview_rejectsBadDates() {
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-12", "2026-10-01", 1.0, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("before");
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-14", 1.0, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("future");
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-08-01", "2026-10-12", 1.0, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("62");
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "01/10/2026", "2026-10-12", 1.0, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("YYYY-MM-DD");
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", null, "2026-10-12", 1.0, null, null, null))
                .isInstanceOf(ToolUserException.class);
    }

    @Test
    void preview_sixtyTwoDayRangeIsTheLimit() {
        givenFarm(1000000.0);

        FeedingPreview p = service.previewFeeding("pond 3", "king fish", "2026-08-12", "2026-10-12", 1.0, null, null, null);

        assertThat(p.days()).isEqualTo(62);
    }

    @Test
    void preview_ambiguousBatchPropagates() {
        when(resolver.resolveBatch("tilapia")).thenThrow(new AmbiguousReferenceException("More than one"));

        assertThatThrownBy(() -> service.previewFeeding("tilapia", "king fish", "2026-10-01", "2026-10-02", 1.0, null, null, null))
                .isInstanceOf(AmbiguousReferenceException.class);
    }

    // ---- preview: stock ----

    @Test
    void preview_insufficientStock_isBlockedWithNoToken() {
        givenFarm(1000.0);

        FeedingPreview p = previewExample();

        assertThat(p.status()).isEqualTo("BLOCKED");
        assertThat(p.previewToken()).isNull();
        assertThat(p.message()).contains("Insufficient").contains("1000").contains("5280");
        assertThat(p.stockAfterKg()).isNull();
    }

    @Test
    void preview_exactlyEnoughStockIsAllowedAndWarnsAboutLowStock() {
        givenFarm(5280.0);

        FeedingPreview p = previewExample();

        assertThat(p.status()).isEqualTo("READY_TO_CONFIRM");
        assertThat(p.stockAfterKg()).isEqualTo(0.0);
        assertThat(p.warnings()).anyMatch(w -> w.toLowerCase().contains("low"));
    }

    @Test
    void preview_noLowStockWarningWhenPlentyRemains() {
        givenFarm(10000.0);

        assertThat(previewExample().warnings()).isEmpty();
    }

    // ---- preview: overlap ----

    @Test
    void preview_overlapIsBlockedByDefaultAndListsConflicts() {
        givenFarm(10000.0);
        givenExistingLogs("2026-10-05", "2026-10-06");

        FeedingPreview p = previewExample();

        assertThat(p.status()).isEqualTo("BLOCKED");
        assertThat(p.previewToken()).isNull();
        assertThat(p.conflicts()).extracting(FeedingWriteMcpService.Conflict::date)
                .containsExactly("2026-10-05", "2026-10-06");
        assertThat(p.message()).contains("already").contains("overlap");
    }

    @Test
    void preview_overlapAddIssuesTokenWithWarning() {
        givenFarm(10000.0);
        givenExistingLogs("2026-10-05");

        FeedingPreview p = service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-12", 11.0, null, null, "add");

        assertThat(p.status()).isEqualTo("READY_TO_CONFIRM");
        assertThat(p.previewToken()).isNotBlank();
        assertThat(p.conflicts()).hasSize(1);
        assertThat(p.warnings()).anyMatch(w -> w.contains("2026-10-05"));
    }

    @Test
    void preview_overlapReplaceIsNotSupportedAndUnknownModeRejected() {
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-12", 11.0, null, null, "replace"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("not supported");
        assertThatThrownBy(() -> service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-12", 11.0, null, null, "maybe"))
                .isInstanceOf(ToolUserException.class);
    }

    // ---- confirm ----

    @Test
    void confirm_callsBulkEndpointOnceWithPreviewedRows() {
        givenFarm(10000.0);
        String token = previewExample().previewToken();
        when(apiClient.bulkFeed(any())).thenReturn(new BulkFeedLogResponse(List.of("a", "b"), 5280.0, 4720.0));

        FeedingResult result = service.confirmFeeding(token);

        ArgumentCaptor<BulkFeedLogRequest> captor = ArgumentCaptor.forClass(BulkFeedLogRequest.class);
        verify(apiClient).bulkFeed(captor.capture());
        BulkFeedLogRequest sent = captor.getValue();
        assertThat(sent.batchId()).isEqualTo("b-3");
        assertThat(sent.feedType()).isEqualTo(FEED);
        assertThat(sent.entries()).hasSize(12);
        assertThat(sent.entries().get(0).date()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(sent.entries()).allSatisfy(e -> assertThat(e.amount()).isEqualTo(440.0));
        assertThat(result.totalKg()).isEqualTo(5280.0);
        assertThat(result.remainingStockKg()).isEqualTo(4720.0);
        assertThat(result.logsCreated()).isEqualTo(2);
        assertThat(result.message()).contains("Oct Catfish");
    }

    @Test
    void confirm_tokenWorksOnlyOnce() {
        givenFarm(10000.0);
        String token = previewExample().previewToken();
        when(apiClient.bulkFeed(any())).thenReturn(new BulkFeedLogResponse(List.of("a"), 5280.0, 4720.0));
        service.confirmFeeding(token);

        assertThatThrownBy(() -> service.confirmFeeding(token)).isInstanceOf(ToolUserException.class);
        verify(apiClient, org.mockito.Mockito.times(1)).bulkFeed(any());
    }

    @Test
    void confirm_madeUpOrMissingTokenNeverReachesTheApi() {

        assertThatThrownBy(() -> service.confirmFeeding("made-up")).isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.confirmFeeding(null)).isInstanceOf(ToolUserException.class);
        verify(apiClient, never()).bulkFeed(any());
    }

    @Test
    void confirm_anotherUsersTokenIsRejected() {
        givenFarm(10000.0);
        String token = previewExample().previewToken();
        loginAs("someone-else");

        assertThatThrownBy(() -> service.confirmFeeding(token)).isInstanceOf(ToolUserException.class);
        verify(apiClient, never()).bulkFeed(any());
    }

    @Test
    void confirm_aTokenFromAnotherToolIsRejected() {
        String foreign = tokens.issue(TENANT, "user-1", "confirm_sample", "not a feeding plan");

        assertThatThrownBy(() -> service.confirmFeeding(foreign)).isInstanceOf(ToolUserException.class);
        verify(apiClient, never()).bulkFeed(any());
    }

    @Test
    void confirm_rechecksOverlapAndRefusesIfLogsAppearedSincePreview() {
        givenFarm(10000.0);
        String token = previewExample().previewToken();
        givenExistingLogs("2026-10-07");

        assertThatThrownBy(() -> service.confirmFeeding(token))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("2026-10-07").hasMessageContaining("preview_feeding");
        verify(apiClient, never()).bulkFeed(any());
    }

    @Test
    void confirm_withOverlapAddDoesNotRecheckOverlap() {
        givenFarm(10000.0);
        givenExistingLogs("2026-10-05");
        String token = service.previewFeeding("pond 3", "king fish", "2026-10-01", "2026-10-12", 11.0, null, null, "add").previewToken();
        when(apiClient.bulkFeed(any())).thenReturn(new BulkFeedLogResponse(List.of("a"), 5280.0, 4720.0));

        service.confirmFeeding(token);

        verify(apiClient).bulkFeed(any());
    }

    @Test
    void confirm_apiFailureTellsUserToPreviewAgain() {
        givenFarm(10000.0);
        String token = previewExample().previewToken();
        when(apiClient.bulkFeed(any())).thenThrow(new ToolUserException("Insufficient global feed stock for 'X'"));

        assertThatThrownBy(() -> service.confirmFeeding(token))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("Insufficient global feed stock")
                .hasMessageContaining("preview_feeding again");
    }
}
