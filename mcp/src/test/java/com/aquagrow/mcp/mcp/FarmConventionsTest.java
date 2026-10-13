package com.aquagrow.mcp.mcp;

import com.aquagrow.mcp.config.FeedingProperties;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FarmConventionsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneId.of("Asia/Kolkata"));

    private static FarmConventions conventions(double bagKg, String currency) {
        return new FarmConventions(new FeedingProperties(bagKg, 5000.0, 62), CLOCK, currency, Duration.ofMinutes(10));
    }

    // ---- content ----

    @Test
    void textStatesTheFarmRulesFromConfiguration() {
        String text = conventions(40.0, "INR").text();

        assertThat(text)
                .contains("One bag of feed = 40 kg")
                .contains("PER DAY")
                .contains("YYYY-MM-DD")
                .contains("GRAMS")
                .contains("INR")
                .contains("62 days")
                .contains("5000 kg per day")
                .contains("10 minutes");
    }

    @Test
    void textIncludesTodayInTheFarmTimezone() {
        // 10:00 UTC on the 13th is 15:30 on the 13th in Kolkata
        assertThat(conventions(40.0, "INR").text()).contains("2026-10-13").contains("Asia/Kolkata");
    }

    @Test
    void workedExampleFollowsTheConfiguredBagSize() {
        assertThat(conventions(40.0, "INR").text()).contains("11 bags per day").contains("440 kg per day").contains("5280 kg");

        String other = conventions(25.0, "INR").text();
        assertThat(other).contains("One bag of feed = 25 kg").contains("275 kg per day").contains("3300 kg");
        assertThat(other).doesNotContain("One bag of feed = 40 kg");
    }

    @Test
    void textSpellsOutTheConfirmationRule() {
        String text = conventions(40.0, "INR").text();

        assertThat(text)
                .contains("preview_feeding").contains("confirm_feeding")
                .contains("preview_sample").contains("confirm_sample")
                .containsIgnoringCase("explicit")
                .containsIgnoringCase("never guess")
                .containsIgnoringCase("tenant");
    }

    // ---- served as a resource ----

    @Test
    void conventionsAreServedAsAResourceAndOtherResourcesAreUnchanged() {
        FarmConventions conventions = conventions(40.0, "INR");
        List<McpServerFeatures.SyncResourceSpecification> resources =
                new AquaGrowResourceProvider().aquaGrowResources(conventions);

        var conventionSpec = resources.stream()
                .filter(r -> r.resource().uri().equals("aquagrow://conventions")).findFirst().orElseThrow();
        var result = conventionSpec.readHandler().apply(null, new McpSchema.ReadResourceRequest("aquagrow://conventions"));
        String served = ((McpSchema.TextResourceContents) result.contents().get(0)).text();
        assertThat(served).isEqualTo(conventions.text());

        var pondsSpec = resources.stream()
                .filter(r -> r.resource().uri().equals("aquagrow://ponds")).findFirst().orElseThrow();
        var pondsResult = pondsSpec.readHandler().apply(null, new McpSchema.ReadResourceRequest("aquagrow://ponds"));
        assertThat(((McpSchema.TextResourceContents) pondsResult.contents().get(0)).text())
                .contains("aquagrow://ponds").doesNotContain("farm conventions");
    }

    // ---- served as a prompt ----

    @Test
    void conventionsAreServedAsAPrompt() {
        FarmConventions conventions = conventions(40.0, "INR");
        List<McpServerFeatures.SyncPromptSpecification> prompts = new AquaGrowPromptProvider(conventions).aquaGrowPrompts();

        var spec = prompts.stream().filter(p -> p.prompt().name().equals("farm_conventions")).findFirst().orElseThrow();
        assertThat(spec.prompt().arguments()).isEmpty();
        var result = spec.promptHandler().apply(null, new McpSchema.GetPromptRequest("farm_conventions", Map.of()));

        assertThat(result.messages()).hasSize(1);
        assertThat(((McpSchema.TextContent) result.messages().get(0).content()).text()).isEqualTo(conventions.text());
        assertThat(prompts).extracting(p -> p.prompt().name()).contains("batch_profitability_analysis");
    }
}
