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
            promptSpec("weekly_farm_review",
                "Weekly review of the whole farm: water quality, growth and feed, mortality, stock, money owed and "
                    + "profit, ending with the most important actions for next week. Analysis only.",
                List.of(
                    arg("weekEnding", "Last day of the week, YYYY-MM-DD (default: today)", false)
                )),
            promptSpec("cost_reduction",
                "Find where the farm's money goes and where costs can realistically be cut, ranked by rupee impact. "
                    + "Analysis only.",
                List.of(
                    arg("from", "Start of the period, YYYY-MM-DD (default: 6 months ago)", false),
                    arg("to", "End of the period, YYYY-MM-DD (default: today)", false)
                )),
            promptSpec("feed_optimization",
                "Check whether feeding matches growth, FCR, stock and water conditions, and suggest adjustments. "
                    + "Analysis only.",
                List.of(
                    arg("batch", "Batch name, id or pond name (default: every active batch)", false)
                )),
            promptSpec("harvest_timing",
                "Estimate when a batch will reach the target weight and whether waiting is worth the extra feed. "
                    + "Analysis only.",
                List.of(
                    arg("batch", "Batch name, id or pond name (default: every active batch)", false),
                    arg("targetWeightGrams", "Target weight per fish in grams (ask the user if not given)", false)
                )),
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
            case "weekly_farm_review" -> {
                String weekEnding = args.get("weekEnding");
                String endLabel = weekEnding == null || weekEnding.isBlank() ? "today" : weekEnding;
                yield """
                    Prepare a weekly farm review for the week ending %s.

                    Analysis only: do not save or change any data, and do not call preview_ or confirm_ tools unless the user asks you to log something afterwards.

                    1. Read the farm conventions (resource aquagrow://conventions) so you use the right units, currency and today's date. If no week ending date was given, use today.
                    2. Operations: call list_active_ponds() and list_batches(). Note the active batches, their ponds and the days since stocking.
                    3. Water quality: call list_monitoring_logs() and look at the last 7 days. Flag readings outside safe ranges (dissolved oxygen below 5 mg/L, pH outside 7.0-8.5, temperature outside 26-32 C, ammonia above 0.02 mg/L) with dates and ponds.
                    4. Growth and feed, for each active batch: call get_feed_efficiency(batch), list_feed_logs_by_batch(batchId) and list_sample_logs_by_batch(batchId). Compare the feed used in the last 7 days with the batch's average daily feed, report the latest average weight and its date, and say whether a growth sample is overdue (none in the last 14 days).
                    5. Mortality: call list_fish_stock_events() and list any MORTALITY events in the last 7 days.
                    6. Stock and payables: call get_low_stock_alerts() and get_outstanding_payables(). Report feed types running low and roughly how many days of feed remain at the recent daily rate. List unpaid feed orders (quantities only, because no prices are recorded) and salaries not yet paid for the current month.
                    7. Money: call get_farm_pnl(from, to) for the current month so far and for last month. Report net revenue, total costs and profit. Say that revenue is net of commission and that salaries are only included for the main farm account.
                    8. Write the review as: a three-line summary; what went well; risks and anything abnormal; and the 3 to 5 most important actions for next week, each with the evidence behind it.

                    Use only numbers returned by the tools, with their units and dates. Pass on the notes the tools return (missing data, provisional figures). If something could not be checked, say so instead of guessing.
                    """.formatted(endLabel);
            }
            case "cost_reduction" -> {
                String from = args.get("from");
                String to = args.get("to");
                String period = (from == null || from.isBlank() || to == null || to.isBlank())
                    ? "the last 6 months ending today (state the exact dates you used)"
                    : from + " to " + to;
                yield """
                    Find where the farm's money goes and where costs can realistically be reduced, for %s.

                    Analysis only: do not save or change any data, and do not call preview_ or confirm_ tools.

                    1. Read the farm conventions (resource aquagrow://conventions) for units, currency and today's date.
                    2. Call get_farm_pnl(from, to) for the period. Study costByCategory and costByMacroInvestment: which categories are largest, which are growing month to month, and how costs compare with net revenue.
                    3. Call list_batches(). For every batch that was harvested in the period, call get_batch_financials(batch) and compare cost per kg harvested, break-even rate and margin between batches. Find what the best batches did differently.
                    4. For the largest cost category (usually feed), call get_feed_efficiency(batch) per batch and compare FCR and feed per kg harvested.
                    5. Call get_outstanding_payables() to see what is still owed and whether anything is overdue.
                    6. Rank the opportunities by rupee impact. For each give: what to change, the evidence (numbers from the tools), the estimated saving and how that estimate was worked out, and what could go wrong.

                    Keep these in mind: salaries are a fixed farm-wide cost and are not a batch cost; batches that are not fully harvested are provisional, so do not compare their profit with completed batches; feed orders have no price recorded. Label every saving as an estimate. Use only numbers returned by the tools, with their units, and pass on the notes the tools return. If the data cannot support a recommendation, say so instead of guessing.
                    """.formatted(period);
            }
            case "feed_optimization" -> {
                String batch = args.get("batch");
                String scope = batch == null || batch.isBlank() ? "every active batch (use list_batches())" : "batch " + batch;
                yield """
                    Check whether feeding matches growth, FCR, stock and water conditions for %s.

                    Analysis only: do not save or change any data, and do not call preview_ or confirm_ tools. Any change you recommend is a suggestion for the user to decide.

                    1. Read the farm conventions (resource aquagrow://conventions) for units and today's date.
                    2. Call get_feed_efficiency(batch): total and daily feed, feed by type, FCR (only available once a batch is fully harvested) and feed per kg harvested.
                    3. Call list_sample_logs_by_batch(batchId) and work out growth in grams per day between samples. Say how many samples and which dates the estimate rests on.
                    4. Call list_feed_logs_by_batch(batchId) and look at the daily pattern: gaps, sudden changes, and whether the amount follows growth.
                    5. Call list_monitoring_logs_by_batch(batchId): low dissolved oxygen or high temperature or ammonia reduce appetite and make extra feed wasteful. Link any feed drop to a water quality event only if the dates support it.
                    6. Call list_feed_stock() and get_low_stock_alerts(): estimate how many days of each feed type remain at the current daily rate.
                    7. Conclude: is feeding too high, too low or about right for the growth seen, and why. Give concrete suggestions (rate, feed type, frequency, restocking) with the evidence behind each, and say what extra data would make the advice firmer.

                    Typical FCR targets depend on the species, so state the target you assume. Use only numbers returned by the tools, with their units and dates, and pass on the notes the tools return. Do not invent growth or feed figures.
                    """.formatted(scope);
            }
            case "harvest_timing" -> {
                String batch = args.get("batch");
                String scope = batch == null || batch.isBlank() ? "every active batch (use list_batches())" : "batch " + batch;
                String target = args.get("targetWeightGrams");
                String targetLine = target == null || target.isBlank()
                    ? "No target weight was given: ask the user for the target weight per fish in grams before estimating dates."
                    : "The target weight is " + target + " g per fish.";
                yield """
                    Estimate when %s will reach the target weight and whether waiting longer is worth the extra feed.

                    Analysis only: do not save or change any data, and do not call preview_ or confirm_ tools.

                    %s

                    1. Read the farm conventions (resource aquagrow://conventions) for units, currency and today's date.
                    2. Call list_sample_logs_by_batch(batchId). Take the latest average weight and the growth in grams per day between recent samples. State how many samples and which dates the growth rate rests on. If there are fewer than 2 samples in the last 30 days, say the estimate is weak and recommend a fresh sample before deciding.
                    3. Days to target = (target weight - latest average weight) / growth per day. Give the date range this implies, not a single date, and note that growth usually slows as fish get larger.
                    4. Call get_batch_financials(batch) for costs so far and the break-even rate per kg, and list_harvest_logs() for the rates actually achieved on earlier harvests.
                    5. Call get_feed_efficiency(batch) for the daily feed and feed cost per kg. Compare the extra feed cost of waiting with the extra weight gained, valued at the recent harvest rates. Say whether waiting pays off, and by how much, as an estimate.
                    6. Check list_monitoring_logs_by_batch(batchId) and list_fish_stock_events_by_batch(batchId) for water quality or mortality problems that argue for harvesting earlier.
                    7. Recommend a harvest window with the reasoning, the main risks, and what would change the advice.

                    Use only numbers returned by the tools, with their units and dates, and pass on the notes the tools return. Label projections as estimates.
                    """.formatted(scope, targetLine);
            }
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
