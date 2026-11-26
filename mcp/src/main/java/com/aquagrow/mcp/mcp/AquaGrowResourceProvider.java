package com.aquagrow.mcp.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Exposes all Aqua Grow entity collections as MCP Resources.
 *
 * URI scheme: aquagrow://{entity}
 *
 * AI clients can enumerate available data sources via these resources.
 * Actual data retrieval is done via the corresponding MCP tools, which are
 * automatically scoped to the caller's authenticated tenant.
 */
@Configuration
public class AquaGrowResourceProvider {

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> aquaGrowResources(FarmConventions conventions) {
        return List.of(
            conventionsSpec(conventions),
            spec("aquagrow://ponds", "Ponds",
                "All ponds in the aquaculture facility. Area, depth, type, status, and GPS coordinates."),
            spec("aquagrow://batches", "Batches",
                "Cultivation batches (fish stocking cycles) with stocking and estimated harvest dates."),
            spec("aquagrow://feed-logs", "Feed Logs",
                "Daily feeding records per batch: feed type and amount in kg."),
            spec("aquagrow://feed-stock", "Feed Stock",
                "Current feed inventory levels with low-stock thresholds."),
            spec("aquagrow://feed-orders", "Feed Orders",
                "Purchase orders for feed: PENDING, CONFIRMED, RECEIVED, CANCELLED."),
            spec("aquagrow://harvest-logs", "Harvest Logs",
                "Harvest events with biomass weight, revenue, buyer, and commission details."),
            spec("aquagrow://monitoring-logs", "Monitoring Logs",
                "Water quality measurements: DO, pH, temperature, ammonia, salinity, turbidity."),
            spec("aquagrow://sample-logs", "Sample Logs",
                "Fish growth sampling records: average weight and survival estimate."),
            spec("aquagrow://employees", "Employees",
                "Employee records: role, salary, advance taken, and contact info."),
            spec("aquagrow://expenditures", "Expenditures",
                "Operational expenditures: Feed, Seed, Salary, Maintenance, Lease, etc."),
            spec("aquagrow://salary-payments", "Salary Payments",
                "Monthly salary payment records per employee including deductions and bonuses."),
            spec("aquagrow://fish-types", "Fish Types",
                "Catalogue of fish/shrimp species used in cultivation batches."),
            spec("aquagrow://fish-seeds", "Fish Seeds",
                "Fish seed stocking records per pond: quantity, size, and price."),
            spec("aquagrow://fish-stock-events", "Fish Stock Events",
                "All fish population events: SEED, HARVEST, MORTALITY, SAMPLING, TRANSFER."),
            spec("aquagrow://suppliers", "Suppliers",
                "Feed and seed supplier directory with contact information.")
        );
    }

    /** The farm's working conventions (units, bag size, confirmation rules), generated fresh on each read. */
    private McpServerFeatures.SyncResourceSpecification conventionsSpec(FarmConventions conventions) {
        String uri = "aquagrow://conventions";
        McpSchema.Resource resource = McpSchema.Resource.builder()
            .uri(uri)
            .name("Farm conventions")
            .description("How this farm measures things (kg, 40 kg bags, grams for samples), how quantities are read "
                + "(per day), today's date, and the preview-then-confirm rule for saving data. "
                + "Read this before logging feed or samples.")
            .mimeType("text/markdown")
            .build();
        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) ->
            new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(uri, "text/markdown", conventions.text()))));
    }

    private McpServerFeatures.SyncResourceSpecification spec(String uri, String name, String description) {
        McpSchema.Resource resource = McpSchema.Resource.builder()
            .uri(uri).name(name).description(description).mimeType("text/plain").build();
        return new McpServerFeatures.SyncResourceSpecification(resource, this::handleRead);
    }

    private McpSchema.ReadResourceResult handleRead(McpSyncServerExchange exchange,
                                                     McpSchema.ReadResourceRequest request) {
        String uri = request.uri();
        String content = """
            Use the corresponding MCP tool to retrieve live data — it is automatically
            scoped to your authenticated tenant.
            Resource URI: %s

            Example tools for this resource:
            - list_ponds()
            - list_batches()
            - list_feed_logs()
            - list_monitoring_logs()
            - list_employees()
            - (and more - see the full tool list)
            """.formatted(uri);
        return new McpSchema.ReadResourceResult(
            List.of(new McpSchema.TextResourceContents(uri, "text/plain", content))
        );
    }
}
