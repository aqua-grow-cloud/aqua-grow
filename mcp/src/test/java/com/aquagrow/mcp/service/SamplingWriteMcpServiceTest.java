package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.AquaGrowApiClient;
import com.aquagrow.mcp.client.SampleLogPayload;
import com.aquagrow.mcp.client.SampleLogResult;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.confirm.PreviewTokenService;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.ResolverService.AmbiguousReferenceException;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import com.aquagrow.mcp.service.ResolverService.ResolvedFishType;
import com.aquagrow.mcp.service.SampleLogLookup.Context;
import com.aquagrow.mcp.service.SampleLogLookup.Existing;
import com.aquagrow.mcp.service.SamplingWriteMcpService.SamplePreview;
import com.aquagrow.mcp.service.SamplingWriteMcpService.SampleResult;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SamplingWriteMcpServiceTest {

    private static final String TENANT = "t1";

    @Mock ResolverService resolver;
    @Mock AquaGrowApiClient apiClient;
    @Mock SampleLogLookup sampleLogLookup;

    private PreviewTokenService tokens;
    private SamplingWriteMcpService service;

    @BeforeEach
    void setUp() {
        tokens = new PreviewTokenService(Duration.ofMinutes(10));
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        service = new SamplingWriteMcpService(resolver, apiClient, sampleLogLookup, tokens, clock);
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

    private void givenFarm() {
        lenient().when(resolver.resolveBatch("pond 3"))
                .thenReturn(new ResolvedBatch("b-3", "Oct Catfish", List.of("Pond 3"), LocalDate.of(2026, 9, 1)));
        lenient().when(resolver.resolveFishType(eq("b-3"), any()))
                .thenReturn(new ResolvedFishType("ft-2", "Catfish", List.of("Pond 3")));
        givenHistory(List.of(), null);
    }

    private void givenHistory(List<Existing> sameDay, Existing previous) {
        lenient().when(sampleLogLookup.context(eq("b-3"), eq("ft-2"), any())).thenReturn(new Context(sameDay, previous));
    }

    private SamplePreview previewExample() {
        return service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, 12.5, 95.0, null);
    }

    // ---- preview ----

    @Test
    void preview_computesAverageWeightAndChangesNothing() {
        givenFarm();

        SamplePreview p = previewExample();

        assertThat(p.status()).isEqualTo("READY_TO_CONFIRM");
        assertThat(p.sampleCount()).isEqualTo(20);
        assertThat(p.totalSampleWeightGrams()).isEqualTo(1000.0);
        assertThat(p.averageWeightGrams()).isEqualTo(50.0);
        assertThat(p.averageLengthCm()).isEqualTo(12.5);
        assertThat(p.survivalEstimatePercent()).isEqualTo(95.0);
        assertThat(p.batchId()).isEqualTo("b-3");
        assertThat(p.batchName()).isEqualTo("Oct Catfish");
        assertThat(p.fishType()).isEqualTo("Catfish");
        assertThat(p.date()).isEqualTo("2026-10-12");
        assertThat(p.stockEventPond()).isEqualTo("Pond 3");
        assertThat(p.previewToken()).isNotBlank();
        assertThat(p.message()).containsIgnoringCase("nothing has been saved").contains("confirm_sample");
        verify(apiClient, never()).createSample(any());
    }

    @Test
    void preview_averageWeightRoundsToTwoDecimals() {
        givenFarm();

        SamplePreview p = service.previewSample("pond 3", "2026-10-12", 3, 100.0, null, null, null, null);

        assertThat(p.averageWeightGrams()).isEqualTo(33.33);
    }

    @Test
    void preview_optionalFieldsAreLeftEmptyNotInvented() {
        givenFarm();

        SamplePreview p = service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, null, null, null);

        assertThat(p.averageLengthCm()).isNull();
        assertThat(p.survivalEstimatePercent()).isNull();
        assertThat(p.warnings()).anyMatch(w -> w.contains("survival") && w.contains("empty"));
    }

    @Test
    void preview_passesFishTypeReferenceToTheResolver() {
        givenFarm();

        service.previewSample("pond 3", "2026-10-12", 20, 1000.0, "catfish", null, null, null);

        verify(resolver).resolveFishType("b-3", "catfish");
    }

    @Test
    void preview_multipleFishTypesWithoutChoice_propagatesQuestion() {
        givenFarm();
        when(resolver.resolveFishType("b-3", null)).thenThrow(new AmbiguousReferenceException("Which fish type"));

        assertThatThrownBy(() -> previewExample()).isInstanceOf(AmbiguousReferenceException.class);
    }

    @Test
    void preview_stockEventPondIsNotApplicableWhenTypeSpansPonds() {
        givenFarm();
        when(resolver.resolveFishType("b-3", null)).thenReturn(new ResolvedFishType("ft-2", "Catfish", List.of("Pond 3", "Pond 4")));

        assertThat(previewExample().stockEventPond()).startsWith("N/A");
    }

    @Test
    void preview_validatesInputs() {
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 0, 1000.0, null, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("sampleCount");
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", null, 1000.0, null, null, null, null))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 10001, 1000.0, null, null, null, null))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 20, 0.0, null, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("grams");
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 20, null, null, null, null, null))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, -1.0, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("length");
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, null, 100.5, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("0 and 100");
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, null, -1.0, null))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, null, null, "replace"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("not supported");
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, null, null, "maybe"))
                .isInstanceOf(ToolUserException.class);
    }

    @Test
    void preview_validatesDates() {
        assertThatThrownBy(() -> service.previewSample("pond 3", "2026-10-14", 20, 1000.0, null, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("future");
        assertThatThrownBy(() -> service.previewSample("pond 3", "12/10/2026", 20, 1000.0, null, null, null, null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("YYYY-MM-DD");
        assertThatThrownBy(() -> service.previewSample("pond 3", null, 20, 1000.0, null, null, null, null))
                .isInstanceOf(ToolUserException.class);
    }

    @Test
    void preview_todayIsAllowed() {
        givenFarm();

        assertThat(service.previewSample("pond 3", "2026-10-13", 20, 1000.0, null, null, null, null).status())
                .isEqualTo("READY_TO_CONFIRM");
    }

    @Test
    void preview_sameDaySampleOfSameFishTypeIsBlockedByDefault() {
        givenFarm();
        givenHistory(List.of(new Existing("2026-10-12", 20, 50.0)), null);

        SamplePreview p = previewExample();

        assertThat(p.status()).isEqualTo("BLOCKED");
        assertThat(p.previewToken()).isNull();
        assertThat(p.duplicates()).hasSize(1);
        assertThat(p.message()).contains("already").contains("overlap");
    }

    @Test
    void preview_overlapAddIssuesTokenWithWarning() {
        givenFarm();
        givenHistory(List.of(new Existing("2026-10-12", 20, 50.0)), null);

        SamplePreview p = service.previewSample("pond 3", "2026-10-12", 20, 1000.0, null, 12.5, 95.0, "add");

        assertThat(p.status()).isEqualTo("READY_TO_CONFIRM");
        assertThat(p.previewToken()).isNotBlank();
        assertThat(p.warnings()).anyMatch(w -> w.contains("already"));
    }

    @Test
    void preview_showsGrowthSincePreviousSample() {
        givenFarm();
        givenHistory(List.of(), new Existing("2026-10-02", 20, 40.0));

        SamplePreview p = previewExample();

        assertThat(p.previousSampleDate()).isEqualTo("2026-10-02");
        assertThat(p.previousAverageWeightGrams()).isEqualTo(40.0);
        assertThat(p.growthGramsPerDay()).isEqualTo(1.0);
        assertThat(p.warnings()).noneMatch(w -> w.contains("differs"));
    }

    @Test
    void preview_warnsWhenWeightLooksLikeAUnitMistake() {
        givenFarm();
        givenHistory(List.of(), new Existing("2026-10-02", 20, 40.0));

        // 2 g average vs 40 g before: probably kg entered as grams
        SamplePreview tooLow = service.previewSample("pond 3", "2026-10-12", 20, 40.0, null, null, null, null);
        assertThat(tooLow.status()).isEqualTo("READY_TO_CONFIRM");
        assertThat(tooLow.warnings()).anyMatch(w -> w.contains("differs") && w.contains("grams"));

        // 500 g average vs 40 g before
        SamplePreview tooHigh = service.previewSample("pond 3", "2026-10-12", 20, 10000.0, null, null, null, null);
        assertThat(tooHigh.warnings()).anyMatch(w -> w.contains("differs"));
    }

    // ---- confirm ----

    @Test
    void confirm_createsTheSampleOnceThroughTheApi() {
        givenFarm();
        String token = previewExample().previewToken();
        when(apiClient.createSample(any())).thenReturn(new SampleLogResult("s-1", "b-3", "ft-2",
                LocalDate.of(2026, 10, 12), 20, 1000.0, 50.0, 12.5, 95.0));

        SampleResult result = service.confirmSample(token);

        ArgumentCaptor<SampleLogPayload> captor = ArgumentCaptor.forClass(SampleLogPayload.class);
        verify(apiClient).createSample(captor.capture());
        SampleLogPayload sent = captor.getValue();
        assertThat(sent.batchId()).isEqualTo("b-3");
        assertThat(sent.fishTypeId()).isEqualTo("ft-2");
        assertThat(sent.date()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(sent.sampleCount()).isEqualTo(20);
        assertThat(sent.totalSampleWeight()).isEqualTo(1000.0);
        assertThat(sent.averageWeight()).isEqualTo(50.0);
        assertThat(sent.averageLength()).isEqualTo(12.5);
        assertThat(sent.survivalEstimate()).isEqualTo(95.0);
        assertThat(result.status()).isEqualTo("SAVED");
        assertThat(result.sampleLogId()).isEqualTo("s-1");
        assertThat(result.message()).contains("Oct Catfish").contains("50");
    }

    @Test
    void confirm_tokenWorksOnlyOnce() {
        givenFarm();
        String token = previewExample().previewToken();
        when(apiClient.createSample(any())).thenReturn(new SampleLogResult("s-1", "b-3", "ft-2", null, 20, 1000.0, 50.0, null, null));
        service.confirmSample(token);

        assertThatThrownBy(() -> service.confirmSample(token)).isInstanceOf(ToolUserException.class);
        verify(apiClient, times(1)).createSample(any());
    }

    @Test
    void confirm_madeUpTokenAnotherUsersTokenAndForeignToolTokenNeverReachTheApi() {
        givenFarm();
        String token = previewExample().previewToken();
        String foreign = tokens.issue(TENANT, "user-1", "confirm_feeding", "not a sample plan");

        assertThatThrownBy(() -> service.confirmSample("made-up")).isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.confirmSample(null)).isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.confirmSample(foreign)).isInstanceOf(ToolUserException.class);
        loginAs("someone-else");
        assertThatThrownBy(() -> service.confirmSample(token)).isInstanceOf(ToolUserException.class);
        verify(apiClient, never()).createSample(any());
    }

    @Test
    void confirm_rechecksForDuplicateAddedSincePreview() {
        givenFarm();
        String token = previewExample().previewToken();
        givenHistory(List.of(new Existing("2026-10-12", 20, 50.0)), null);

        assertThatThrownBy(() -> service.confirmSample(token))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("preview_sample");
        verify(apiClient, never()).createSample(any());
    }

    @Test
    void confirm_apiFailureTellsUserToPreviewAgain() {
        givenFarm();
        String token = previewExample().previewToken();
        when(apiClient.createSample(any())).thenThrow(new ToolUserException("Sample count must be greater than zero"));

        assertThatThrownBy(() -> service.confirmSample(token))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("Sample count must be greater than zero")
                .hasMessageContaining("preview_sample again");
    }
}
