package com.aquagrow.mcp.mcp;

import com.aquagrow.mcp.config.FeedingProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;

/**
 * The farm's working conventions, written for the model: units, bag size, how quantities
 * are read, and the preview-then-confirm rule for writes. Values come from configuration,
 * and today's date is included because the model does not know it.
 *
 * This is guidance only. The rules that matter are enforced by the tools themselves
 * (single-use preview tokens, tenant from the login token, stock checks in the API).
 */
@Component
public class FarmConventions {

    private static final int EXAMPLE_BAGS = 11;
    private static final int EXAMPLE_DAYS = 12;

    private final FeedingProperties feeding;
    private final Clock clock;
    private final String currency;
    private final Duration previewTtl;

    public FarmConventions(FeedingProperties feeding, Clock clock,
                           @Value("${aquagrow.mcp.conventions.currency:INR}") String currency,
                           @Value("${aquagrow.mcp.preview-ttl:PT10M}") Duration previewTtl) {
        this.feeding = feeding;
        this.clock = clock;
        this.currency = currency;
        this.previewTtl = previewTtl;
    }

    public String currency() {
        return currency;
    }

    public String text() {
        double bag = feeding.defaultBagSizeKg();
        BigDecimal perDay = BigDecimal.valueOf(bag).multiply(BigDecimal.valueOf(EXAMPLE_BAGS));
        BigDecimal total = perDay.multiply(BigDecimal.valueOf(EXAMPLE_DAYS));

        return """
                # Aqua Grow farm conventions

                Today is %s (timezone %s). Write every date as YYYY-MM-DD.

                ## Units
                - Feed is measured in kilograms (kg). One bag of feed = %s kg. If the user names a different bag size, pass it as bagSizeKg.
                - Fish sample weights are in GRAMS (g) and lengths in centimetres (cm). Convert before calling a tool: 2 kg = 2000 g.
                - Money is in %s.

                ## Feeding quantities
                - A feeding quantity is PER DAY for every date in an inclusive date range.
                  Example: "%d bags per day from 2026-10-01 to 2026-10-12" = %d bags x %s kg = %s kg per day for %d days = %s kg in total.
                - If the user gives a quantity for a whole period and it is unclear whether it is per day or a total, ask. Totals over a range are not supported: get the per-day amount confirmed.
                - At most %d days and %s kg per day can be logged in one request.

                ## Saving data is always two steps
                1. Call preview_feeding or preview_sample. This saves nothing.
                2. Show the user the plan (batch, feed type or fish type, dates, quantities, totals, stock, and every warning) and ask for their explicit approval.
                3. Only after the user clearly says yes in this conversation, call confirm_feeding or confirm_sample with the previewToken.
                - Never confirm on your own initiative, never reuse a token, and never confirm something the user has not seen. A preview expires after %d minutes; if it fails, preview again and ask again.
                - If a preview is BLOCKED (not enough stock, entries already exist on those dates), explain why and let the user decide. Do not work around it: use overlap='add' only if the user says they want extra entries on top of existing ones.

                ## Never guess
                - If the batch, feed type, fish type, dates, quantity or bag size is missing or ambiguous, ask the user. When a tool lists several candidates, ask which one is meant.
                - Batches can be named by batch name or by pond name (for example "Pond 3").
                - Do not invent numbers for analysis: quote what the tools return, with their units and date ranges, and mention any data-quality notes.

                ## Money and analysis
                - Harvest revenue is NET of commission. Salaries are a farm-wide fixed cost and are never allocated to a batch.
                - A batch is "provisional" until all its fish are harvested: do not present its profit as final.
                - Feed orders have no price recorded, so unpaid orders are shown by quantity only. Say so rather than estimating an amount.
                - Tool results carry notes about data quality (missing data, double-counting guards). Pass the relevant ones on to the user.

                ## Access
                - Every tool is automatically limited to the signed-in user's own farm data. Never ask for or pass a tenant or account id.
                """.formatted(
                LocalDate.now(clock), clock.getZone(),
                fmt(bag), currency,
                EXAMPLE_BAGS, EXAMPLE_BAGS, fmt(bag), fmt(perDay.doubleValue()), EXAMPLE_DAYS, fmt(total.doubleValue()),
                feeding.maxDays(), fmt(feeding.maxKgPerDay()),
                previewTtl.toMinutes());
    }

    private static String fmt(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
