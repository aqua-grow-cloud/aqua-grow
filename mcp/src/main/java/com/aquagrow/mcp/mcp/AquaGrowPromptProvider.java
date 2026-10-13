package com.aquagrow.mcp.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * MCP Prompts for aquaculture analysis and operational tasks.
 * These prompt templates guide AI agents to perform structured analyses
 * using the Aqua Grow MCP tools.
 *
 * Tenant scoping is implicit: every referenced tool derives the tenant
 * from the caller's authenticated session, so prompts no longer accept
 * or pass a tenantId argument.
 */
@Configuration
public class AquaGrowPromptProvider {

    private final FarmConventions conventions;

    public AquaGrowPromptProvider(FarmConventions conventions) {
        this.conventions = conventions;
    }

    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> aquaGrowPrompts() {
        return List.of(
            promptSpec("farm_conventions",
                "Load this farm's conventions (kg and 40 kg bags, per-day feeding, grams for samples, "
                    + "preview-then-confirm for saving). Use before logging feed or samples.",
                List.of()),
            promptSpec("batch_profitability_analysis",
                "Analyse the profitability (revenue, costs, ROI, FCR) of a cultivation batch",
                List.of(
                    arg("batchId", "The batch ID to analyse", true)
                )),
            promptSpec("water_quality_assessment",
                "Assess water quality trends and flag out-of-range parameters for a batch",
                List.of(
                    arg("batchId", "The batch ID to assess", true)
                )),
            promptSpec("feed_consumption_report",
                "Generate a feed consumption and Feed Conversion Ratio (FCR) report",
                List.of(
                    arg("batchId", "The batch ID", true)
                )),
            promptSpec("inventory_reorder_check",
                "Check feed inventory levels and suggest reorders for low-stock items",
                List.of()),
            promptSpec("farm_dashboard_summary",
                "Generate a comprehensive farm status overview: ponds, batches, stock, feed, alerts",
                List.of()),
            promptSpec("harvest_planning",
                "Plan upcoming harvest schedule based on current fish weight vs target",
                List.of(
                    arg("targetWeightGrams", "Target harvest weight per fish in grams (optional)", false)
                )),
            promptSpec("employee_payroll_summary",
                "Generate monthly payroll summary with net payable amounts per employee",
                List.of(
                    arg("month", "Month in YYYY-MM format (e.g. 2025-07)", true)
                )),
            promptSpec("mortality_analysis",
                "Analyse fish mortality events and correlate with water quality data",
                List.of(
                    arg("batchId", "Optional batch ID to scope the analysis", false)
                ))
        );
    }

    private McpServerFeatures.SyncPromptSpecification promptSpec(String name, String description,
                                                                   List<McpSchema.PromptArgument> arguments) {
        McpSchema.Prompt prompt = new McpSchema.Prompt(name, description, arguments);
        return new McpServerFeatures.SyncPromptSpecification(prompt, this::handlePrompt);
    }

    private McpSchema.GetPromptResult handlePrompt(McpSyncServerExchange exchange,
                                                    McpSchema.GetPromptRequest request) {
        String name = request.name();
        Map<String, String> args = request.arguments() != null
            ? request.arguments().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() != null ? e.getValue().toString() : ""))
            : Map.of();
        String userMessage = buildMessage(name, args);

        return new McpSchema.GetPromptResult(
            "Aqua Grow aquaculture analysis: " + name,
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(userMessage)
            ))
        );
    }

    private String buildMessage(String name, Map<String, String> args) {
        return switch (name) {
            case "farm_conventions" -> conventions.text();
            case "batch_profitability_analysis" -> {
                String batchId = args.getOrDefault("batchId", "<batchId>");
                yield """
                    Analyse the profitability of batch %s.

                    1. Call get_batch(batchId="%s") for batch details
                    2. Call list_expenditures_by_batch(batchId="%s") for all costs
                    3. Call list_harvest_logs_by_batch(batchId="%s") for revenues
                    4. Calculate: Net Profit = Total Revenue - Total Expenditure
                    5. Calculate: ROI = (Net Profit / Total Expenditure) × 100%%
                    6. Call list_feed_logs_by_batch + list_sample_logs_by_batch to compute FCR
                    7. Present a structured profitability summary with recommendations.
                    """.formatted(batchId, batchId, batchId, batchId);
            }
            case "water_quality_assessment" -> {
                String batchId = args.getOrDefault("batchId", "<batchId>");
                yield """
                    Assess water quality for batch %s.

                    1. Call list_monitoring_logs_by_batch(batchId="%s")
                    2. Check each reading against safe aquaculture ranges:
                       • DO: > 5 mg/L (critical < 3 mg/L)
                       • pH: 7.0–8.5
                       • Temperature: 26–32°C
                       • Ammonia: < 0.02 mg/L
                    3. Identify out-of-range readings with dates
                    4. Provide trend analysis and corrective action recommendations
                    """.formatted(batchId, batchId);
            }
            case "feed_consumption_report" -> {
                String batchId = args.getOrDefault("batchId", "<batchId>");
                yield """
                    Generate feed consumption report for batch %s.

                    1. Call list_feed_logs_by_batch(batchId="%s")
                    2. Call list_sample_logs_by_batch(batchId="%s")
                    3. Calculate total feed consumed (kg)
                    4. Estimate biomass gain from sample weight trends
                    5. Compute FCR = Total Feed / Biomass Gain (target FCR < 1.5 for fish)
                    6. Show daily/weekly feeding rate trends and recommendations
                    """.formatted(batchId, batchId, batchId);
            }
            case "inventory_reorder_check" -> """
                    Check feed inventory and suggest reorders.

                    1. Call list_feed_stock() — identify low/critical stock items
                    2. Call list_pending_feed_orders() — check if orders are already placed
                    3. Call list_suppliers() — identify preferred suppliers per feed type
                    4. Call list_feed_logs() — estimate daily consumption rate
                    5. For each low-stock item without a pending order, recommend reorder quantity
                    """;
            case "farm_dashboard_summary" -> """
                    Generate a comprehensive farm status summary.

                    1. list_active_ponds() — total active ponds and capacity
                    2. list_batches() — active batches and their status
                    3. list_feed_stock() — inventory health and alerts
                    4. list_pending_feed_orders() — outstanding orders
                    5. list_monitoring_logs() — recent water quality (last 7 days)
                    6. Present as an executive dashboard summary with action items
                    """;
            case "harvest_planning" -> {
                String targetWeight = args.getOrDefault("targetWeightGrams", "500");
                yield """
                    Plan harvest schedule (target: %sg per fish).

                    1. Call list_batches() — find all active batches
                    2. For each active batch, call list_sample_logs_by_batch for latest avg weight
                    3. Compare current avg weight vs target %sg
                    4. Estimate days to reach target using weight gain trend
                    5. Rank batches by harvest readiness
                    6. Estimate expected revenue using historical rates from list_harvest_logs
                    """.formatted(targetWeight, targetWeight);
            }
            case "employee_payroll_summary" -> {
                String month = args.getOrDefault("month", "<YYYY-MM>");
                yield """
                    Generate payroll summary for %s.

                    1. Call list_active_employees()
                    2. For each employee, call list_salary_payments_by_employee to check payments for %s
                    3. Calculate: Net Pay = Base Salary - Advance Deduction + Bonus
                    4. Flag employees with outstanding advance balances
                    5. Present a payroll table with totals and unpaid advance summary
                    """.formatted(month, month);
            }
            case "mortality_analysis" -> {
                String batchId = args.get("batchId");
                if (batchId != null && !batchId.isBlank()) {
                    yield """
                        Analyse fish mortality for batch %s.

                        1. Call list_fish_stock_events_by_batch(batchId="%s")
                        2. Filter for type=MORTALITY events
                        3. Calculate cumulative mortality count and mortality rate
                        4. Correlate dates with water quality from list_monitoring_logs_by_batch
                        5. Identify likely causes and recommend preventive measures
                        """.formatted(batchId, batchId);
                } else {
                    yield """
                        Analyse fish mortality across all batches.

                        1. Call list_fish_stock_events()
                        2. Filter for type=MORTALITY events
                        3. Group by batch and pond, calculate mortality rates
                        4. Identify batches/ponds with abnormally high mortality
                        5. Provide summary with root cause analysis and recommendations
                        """;
                }
            }
            default -> "Execute the %s analysis using the available Aqua Grow MCP tools."
                .formatted(name);
        };
    }

    private McpSchema.PromptArgument arg(String name, String description, boolean required) {
        return new McpSchema.PromptArgument(name, description, required);
    }
}
