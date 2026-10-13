package com.aquagrow.mcp.mcp;

import com.aquagrow.mcp.AquaGrowMcpApplication;
import com.aquagrow.mcp.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class WriteToolFilteringTest {

    /** Tools that bypassed aqua-grow-api business rules and were deleted outright. */
    private static final Set<String> REMOVED = Set.of(
            "create_feed_log", "update_feed_stock", "create_sample_log",
            "delete_pond", "delete_batch");

    private static <T> T service(Class<T> type) throws Exception {
        Constructor<?> ctor = type.getDeclaredConstructors()[0];
        Object[] args = Arrays.stream(ctor.getParameterTypes())
                .map(Mockito::mock)
                .toArray();
        return type.cast(ctor.newInstance(args));
    }

    private static Set<String> registeredTools(boolean legacyEnabled) throws Exception {
        ToolCallbackProvider provider = new AquaGrowMcpApplication().aquaGrowToolCallbacks(
                service(PondMcpService.class),
                service(BatchMcpService.class),
                service(FeedMcpService.class),
                service(MonitoringMcpService.class),
                service(HarvestMcpService.class),
                service(HRFinanceMcpService.class),
                service(CatalogMcpService.class),
                legacyEnabled);
        return Arrays.stream(provider.getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .map(d -> d.name())
                .collect(Collectors.toSet());
    }

    @Test
    void defaultRegistryExposesOnlyReadTools() throws Exception {
        Set<String> tools = registeredTools(false);

        assertThat(tools).isNotEmpty();
        assertThat(tools).allSatisfy(name -> assertThat(name)
                .doesNotStartWith("create_")
                .doesNotStartWith("update_")
                .doesNotStartWith("delete_"));
        assertThat(tools).contains("list_ponds", "list_feed_stock", "list_feed_logs",
                "list_sample_logs", "list_harvest_logs", "list_expenditures");
    }

    @Test
    void removedToolsAreNeverRegisteredEvenWhenLegacyToolsAreEnabled() throws Exception {
        assertThat(registeredTools(false)).doesNotContainAnyElementsOf(REMOVED);
        assertThat(registeredTools(true)).doesNotContainAnyElementsOf(REMOVED);
    }

    @Test
    void enablingLegacyFlagRestoresRemainingWriteTools() throws Exception {
        Set<String> tools = registeredTools(true);

        assertThat(tools).contains("create_pond", "create_expenditure", "create_salary_payment",
                "create_harvest_log", "create_monitoring_log", "create_feed_order");
    }

    @Test
    void confirmToolsAreNotTreatedAsLegacyWriteTools() {
        List.of("confirm_feeding", "confirm_sample", "preview_feeding").forEach(name ->
                assertThat(WriteToolFilteringProvider.isLegacyWriteTool(name)).isFalse());
        List.of("create_x", "update_x", "delete_x").forEach(name ->
                assertThat(WriteToolFilteringProvider.isLegacyWriteTool(name)).isTrue());
    }
}
