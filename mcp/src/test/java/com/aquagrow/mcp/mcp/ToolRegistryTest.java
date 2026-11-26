package com.aquagrow.mcp.mcp;

import com.aquagrow.mcp.config.FeedingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryTest {

    /** Every read tool that existed before the move to the API: names are part of the contract with prompts and users. */
    private static final Set<String> READ_TOOLS = Set.of(
            "list_ponds", "get_pond", "list_active_ponds",
            "list_batches", "get_batch",
            "list_feed_logs", "list_feed_logs_by_batch", "list_feed_stock", "get_low_stock_alerts",
            "list_feed_orders", "list_pending_feed_orders",
            "list_monitoring_logs", "list_monitoring_logs_by_batch", "list_sample_logs", "list_sample_logs_by_batch",
            "list_harvest_logs", "list_harvest_logs_by_batch", "list_fish_stock_events",
            "list_fish_stock_events_by_batch", "get_total_revenue_by_batch",
            "list_employees", "list_active_employees", "list_salary_payments", "list_salary_payments_by_employee",
            "list_expenditures", "list_expenditures_by_batch", "get_total_expenditure_by_batch",
            "list_fish_types", "list_fish_seeds", "list_fish_seeds_by_pond", "list_suppliers");

    /** Tools that wrote straight to the database and bypassed the API's business rules. They no longer exist. */
    private static final Set<String> REMOVED = Set.of(
            "create_feed_log", "update_feed_stock", "create_sample_log", "delete_pond", "delete_batch",
            "create_feed_order", "update_feed_order_status", "create_pond", "update_pond_status", "create_batch",
            "create_fish_type", "create_fish_seed", "create_supplier", "create_employee", "update_employee_advance",
            "create_salary_payment", "create_expenditure", "create_harvest_log", "create_monitoring_log");

    @Test
    void allThirtyOneReadToolsAreStillRegistered() throws Exception {
        assertThat(READ_TOOLS).hasSize(31);
        assertThat(TestToolRegistry.registeredTools()).containsAll(READ_TOOLS);
    }

    @Test
    void theOnlyToolsThatChangeDataAreTheConfirmPair() throws Exception {
        Set<String> tools = TestToolRegistry.registeredTools();

        assertThat(tools).noneMatch(n -> n.startsWith("create_") || n.startsWith("update_") || n.startsWith("delete_"));
        assertThat(tools.stream().filter(n -> n.startsWith("confirm_")).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("confirm_feeding", "confirm_sample");
        assertThat(tools).contains("preview_feeding", "preview_sample");
    }

    @Test
    void theLegacyDirectWriteToolsAreGone() throws Exception {
        assertThat(TestToolRegistry.registeredTools()).doesNotContainAnyElementsOf(REMOVED);
    }

    @Test
    void financeToolsAreRegistered() throws Exception {
        assertThat(TestToolRegistry.registeredTools())
                .contains("get_batch_financials", "get_farm_pnl", "get_outstanding_payables", "get_feed_efficiency");
    }

    @Test
    void theRegistryHasExactlyTheExpectedTools() throws Exception {
        // 31 read + 4 finance + 2 preview + 2 confirm
        assertThat(TestToolRegistry.registeredTools()).hasSize(39);
    }

    @Test
    void feedingAndSamplingToolSchemasMarkOnlyTheUnambiguousInputsAsRequired() throws Exception {
        ToolCallbackProvider provider = TestToolRegistry.provider();
        var byName = Arrays.stream(provider.getToolCallbacks())
                .collect(Collectors.toMap(c -> c.getToolDefinition().name(), c -> c.getToolDefinition()));
        var mapper = new tools.jackson.databind.ObjectMapper();

        var feeding = mapper.readTree(byName.get("preview_feeding").inputSchema());
        var required = new java.util.HashSet<String>();
        feeding.get("required").forEach(n -> required.add(n.asText()));
        assertThat(required).containsExactlyInAnyOrder("batch", "feedType", "startDate", "endDate");
        assertThat(feeding.get("properties").propertyNames())
                .contains("bagsPerDay", "kgPerDay", "bagSizeKg", "overlap", "time");
        assertThat(required).doesNotContain("time");
        assertThat(feeding.get("properties").get("time").get("description").asText()).contains("HH:mm");
        assertThat(feeding.get("properties").get("bagsPerDay").get("description").asText()).contains("PER DAY");

        var confirm = mapper.readTree(byName.get("confirm_feeding").inputSchema());
        assertThat(confirm.get("required").get(0).asText()).isEqualTo("previewToken");
        assertThat(byName.get("preview_feeding").description()).contains("DOES NOT SAVE");
        assertThat(byName.get("confirm_feeding").description()).contains("ONLY after the user has clearly approved");

        var sample = mapper.readTree(byName.get("preview_sample").inputSchema());
        var sampleRequired = new java.util.HashSet<String>();
        sample.get("required").forEach(n -> sampleRequired.add(n.asText()));
        assertThat(sampleRequired).containsExactlyInAnyOrder("batch", "date", "sampleCount", "totalSampleWeightGrams");
        assertThat(byName.get("preview_sample").description()).contains("DOES NOT SAVE").contains("GRAMS");
        assertThat(byName.get("confirm_sample").description()).contains("ONLY after the user has clearly approved");
    }

    @Test
    void byBatchToolsAcceptANameNotJustAnId() throws Exception {
        var byName = Arrays.stream(TestToolRegistry.provider().getToolCallbacks())
                .collect(Collectors.toMap(c -> c.getToolDefinition().name(), c -> c.getToolDefinition()));

        for (String name : Set.of("list_feed_logs_by_batch", "list_monitoring_logs_by_batch", "list_sample_logs_by_batch",
                "list_harvest_logs_by_batch", "list_fish_stock_events_by_batch", "list_expenditures_by_batch",
                "get_total_revenue_by_batch", "get_total_expenditure_by_batch", "get_batch")) {
            assertThat(byName.get(name).inputSchema()).as(name).contains("pond name");
        }
    }

    @Test
    void conventionsOnlyMentionWriteToolsThatExist() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
        String text = new FarmConventions(new FeedingProperties(40.0, 5000.0, 62), clock, "INR", Duration.ofMinutes(10)).text();
        Set<String> registered = TestToolRegistry.registeredTools();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(?:preview|confirm)_[a-z_]+\\b").matcher(text);
        int mentioned = 0;
        while (m.find()) {
            mentioned++;
            assertThat(registered).as("tool named in conventions: %s", m.group()).contains(m.group());
        }
        assertThat(mentioned).isGreaterThanOrEqualTo(4);
    }
}
