package com.aquagrow.mcp.mcp;

import com.aquagrow.mcp.config.FeedingProperties;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisPromptsTest {

    private static final Pattern TOOL_NAME =
            Pattern.compile("\\b(?:get|list|create|update|delete|preview|confirm)_[a-z_]+\\b");

    private static final List<String> NEW_PROMPTS =
            List.of("weekly_farm_review", "cost_reduction", "feed_optimization", "harvest_timing");

    private final List<McpServerFeatures.SyncPromptSpecification> prompts = new AquaGrowPromptProvider(
            new FarmConventions(new FeedingProperties(40.0, 5000.0, 62),
                    Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC), "INR", Duration.ofMinutes(10)))
            .aquaGrowPrompts();

    private McpServerFeatures.SyncPromptSpecification spec(String name) {
        return prompts.stream().filter(p -> p.prompt().name().equals(name)).findFirst().orElseThrow();
    }

    private String render(String name, Map<String, Object> args) {
        var result = spec(name).promptHandler().apply(null, new McpSchema.GetPromptRequest(name, args));
        assertThat(result.messages()).hasSize(1);
        return ((McpSchema.TextContent) result.messages().get(0).content()).text();
    }

    /** Fills every declared argument with a recognisable value. */
    private String renderWithAllArguments(String name) {
        Map<String, Object> args = new HashMap<>();
        spec(name).prompt().arguments().forEach(a -> args.put(a.name(), "SAMPLE-" + a.name()));
        return render(name, args);
    }

    @Test
    void theFourAnalysisPromptsAreRegisteredWithTheirArguments() {
        assertThat(prompts).extracting(p -> p.prompt().name()).containsAll(NEW_PROMPTS);

        assertThat(spec("weekly_farm_review").prompt().arguments()).extracting(McpSchema.PromptArgument::name)
                .containsExactly("weekEnding");
        assertThat(spec("cost_reduction").prompt().arguments()).extracting(McpSchema.PromptArgument::name)
                .containsExactly("from", "to");
        assertThat(spec("feed_optimization").prompt().arguments()).extracting(McpSchema.PromptArgument::name)
                .containsExactly("batch");
        assertThat(spec("harvest_timing").prompt().arguments()).extracting(McpSchema.PromptArgument::name)
                .containsExactly("batch", "targetWeightGrams");
        NEW_PROMPTS.forEach(n -> assertThat(spec(n).prompt().arguments())
                .as("%s arguments are all optional", n).allMatch(a -> !Boolean.TRUE.equals(a.required())));
    }

    @Test
    void everyToolNamedInAnyPromptIsActuallyRegistered() throws Exception {
        Set<String> registered = TestToolRegistry.registeredTools(false);
        int checked = 0;
        for (var p : prompts) {
            String name = p.prompt().name();
            for (String text : List.of(renderWithAllArguments(name), render(name, Map.of()))) {
                Matcher m = TOOL_NAME.matcher(text);
                while (m.find()) {
                    checked++;
                    assertThat(registered).as("tool '%s' named in prompt '%s'", m.group(), name).contains(m.group());
                }
            }
        }
        assertThat(checked).isGreaterThan(50);
    }

    @Test
    void analysisPromptsAreReadOnly() {
        for (String name : NEW_PROMPTS) {
            String text = renderWithAllArguments(name);
            Matcher writeTool = Pattern.compile("\\b(?:create|update|delete|preview|confirm)_[a-z_]+\\b").matcher(text);
            assertThat(writeTool.find()).as("%s must not name a write tool, found %s", name,
                    writeTool.find(0) ? writeTool.group() : "none").isFalse();
            assertThat(text).as(name).contains("preview_ or confirm_ tools");
            assertThat(text).as(name).containsIgnoringCase("analysis only");
        }
    }

    @Test
    void analysisPromptsSendTheModelToTheConventionsAndAskForHonestNumbers() {
        for (String name : NEW_PROMPTS) {
            String text = renderWithAllArguments(name);
            assertThat(text).as(name).contains("aquagrow://conventions");
            assertThat(text).as(name).containsIgnoringCase("only numbers returned by the tools");
            assertThat(text).as(name).containsIgnoringCase("notes");
        }
    }

    @Test
    void weeklyReviewCoversTheWholeFarm() {
        String text = render("weekly_farm_review", Map.of("weekEnding", "2026-10-11"));

        assertThat(text).contains("2026-10-11")
                .contains("list_monitoring_logs").contains("get_feed_efficiency").contains("get_low_stock_alerts")
                .contains("get_outstanding_payables").contains("get_farm_pnl").contains("list_fish_stock_events");
        assertThat(render("weekly_farm_review", Map.of())).contains("today");
    }

    @Test
    void costReductionUsesThePnlAndRanksByImpactWithoutInventingSavings() {
        String text = render("cost_reduction", Map.of("from", "2026-04-01", "to", "2026-09-30"));

        assertThat(text).contains("2026-04-01").contains("2026-09-30")
                .contains("get_farm_pnl").contains("get_batch_financials").contains("get_feed_efficiency")
                .containsIgnoringCase("rupee impact").containsIgnoringCase("estimate");
        assertThat(render("cost_reduction", Map.of())).containsIgnoringCase("last 6 months");
    }

    @Test
    void feedOptimizationLooksAtFcrGrowthStockAndWaterQuality() {
        String text = render("feed_optimization", Map.of("batch", "Pond 3"));

        assertThat(text).contains("Pond 3").contains("get_feed_efficiency").contains("list_sample_logs_by_batch")
                .contains("list_feed_logs_by_batch").contains("list_monitoring_logs_by_batch").contains("get_low_stock_alerts")
                .containsIgnoringCase("suggestion");
        assertThat(render("feed_optimization", Map.of())).containsIgnoringCase("every active batch");
    }

    @Test
    void harvestTimingNeedsATargetWeightAndWarnsAboutWeakGrowthData() {
        String text = render("harvest_timing", Map.of("batch", "Pond 3", "targetWeightGrams", "750"));

        assertThat(text).contains("Pond 3").contains("750").contains("list_sample_logs_by_batch")
                .contains("get_batch_financials").contains("list_harvest_logs").contains("get_feed_efficiency")
                .containsIgnoringCase("fewer than 2 samples");
        assertThat(render("harvest_timing", Map.of())).containsIgnoringCase("ask the user for the target weight");
    }

    @Test
    void existingPromptsStillWork() {
        assertThat(render("batch_profitability_analysis", Map.of("batchId", "b-1"))).contains("b-1");
        assertThat(prompts).extracting(p -> p.prompt().name())
                .contains("farm_conventions", "harvest_planning", "mortality_analysis");
    }
}
