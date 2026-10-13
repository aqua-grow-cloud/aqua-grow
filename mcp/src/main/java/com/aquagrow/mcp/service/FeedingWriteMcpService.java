package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.AquaGrowApiClient;
import com.aquagrow.mcp.client.BulkFeedLogRequest;
import com.aquagrow.mcp.client.BulkFeedLogResponse;
import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.config.FeedingProperties;
import com.aquagrow.mcp.confirm.PreviewTokenService;
import com.aquagrow.mcp.model.FeedStock;
import com.aquagrow.mcp.security.CurrentUser;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Feeding writes behind a preview, then explicit confirmation.
 *
 * {@code preview_feeding} computes the plan and changes nothing; {@code confirm_feeding}
 * accepts only the token a preview issued (single use, same user, short-lived) and saves
 * through aqua-grow-api's bulk endpoint so stock deduction and tenant rules apply.
 * A quantity is always per day for every date in the inclusive range.
 */
@Service
@Slf4j
public class FeedingWriteMcpService {

    static final String CONFIRM_TOOL = "confirm_feeding";
    private static final String READY = "READY_TO_CONFIRM";
    private static final String BLOCKED = "BLOCKED";

    public record Row(String date, double kg) {
    }

    public record Conflict(String date, String feedType, Double amountKg) {
    }

    public record FeedingPreview(
            String status,
            String message,
            String batchId,
            String batchName,
            List<String> pondNames,
            String feedType,
            String startDate,
            String endDate,
            int days,
            Double bagsPerDay,
            double bagSizeKg,
            double kgPerDay,
            double totalKg,
            double totalBags,
            Double stockBeforeKg,
            Double stockAfterKg,
            List<Row> rows,
            List<String> warnings,
            List<Conflict> conflicts,
            String previewToken,
            Integer expiresInMinutes) {
    }

    public record FeedingResult(
            String status,
            String message,
            String batchName,
            String feedType,
            String startDate,
            String endDate,
            int logsCreated,
            double totalKg,
            double remainingStockKg) {
    }

    /** What a preview stores server-side and a confirm later executes. */
    public record FeedingPlan(
            String batchId,
            String batchName,
            String feedType,
            List<BulkFeedLogRequest.Entry> entries,
            double totalKg,
            boolean allowOverlap) {
    }

    private final ResolverService resolver;
    private final AquaGrowApiClient apiClient;
    private final FarmDataClient farmData;
    private final FeedLogLookup feedLogLookup;
    private final PreviewTokenService previewTokens;
    private final Clock clock;
    private final FeedingProperties properties;

    public FeedingWriteMcpService(ResolverService resolver, AquaGrowApiClient apiClient, FarmDataClient farmData,
                                  FeedLogLookup feedLogLookup, PreviewTokenService previewTokens, Clock clock, FeedingProperties properties) {
        this.resolver = resolver;
        this.apiClient = apiClient;
        this.farmData = farmData;
        this.feedLogLookup = feedLogLookup;
        this.previewTokens = previewTokens;
        this.clock = clock;
        this.properties = properties;
    }

    @Tool(name = "preview_feeding",
          description = "Step 1 of 2 for logging feed. Works out a feeding plan and shows what would change, but "
                  + "DOES NOT SAVE ANYTHING. The quantity is PER DAY and is applied to every date from startDate to "
                  + "endDate inclusive (e.g. '11 bags per day from 2026-10-01 to 2026-10-12' = 12 daily entries). "
                  + "Give either bagsPerDay or kgPerDay, not both. Never guess the batch, feed type or quantity: ask "
                  + "the user if anything is missing or ambiguous. Show the returned plan to the user and wait for "
                  + "their explicit approval before calling confirm_feeding.")
    public FeedingPreview previewFeeding(
            @ToolParam(description = "Batch name, batch id, or pond name (e.g. 'Pond 3')") String batch,
            @ToolParam(description = "Feed type name as in stock (e.g. 'King Fish Feed')") String feedType,
            @ToolParam(description = "First feeding date, YYYY-MM-DD") String startDate,
            @ToolParam(description = "Last feeding date inclusive, YYYY-MM-DD (same as startDate for one day)") String endDate,
            @ToolParam(description = "Bags fed PER DAY. Use this or kgPerDay.", required = false) Double bagsPerDay,
            @ToolParam(description = "Kilograms fed PER DAY. Use this or bagsPerDay.", required = false) Double kgPerDay,
            @ToolParam(description = "Weight of one bag in kg. Defaults to the farm's standard bag size.", required = false) Double bagSizeKg,
            @ToolParam(description = "What to do if feed logs already exist in the range: 'block' (default) or 'add' "
                    + "(only if the user explicitly wants extra entries on top).", required = false) String overlap) {

        // ---- pure input validation first: nothing is looked up for a bad request ----
        LocalDate from = parseDate(startDate, "startDate");
        LocalDate to = parseDate(endDate, "endDate");
        if (to.isBefore(from)) {
            throw new ToolUserException("endDate (" + to + ") is before startDate (" + from + ").");
        }
        LocalDate today = LocalDate.now(clock);
        if (to.isAfter(today)) {
            throw new ToolUserException("endDate " + to + " is in the future (today is " + today
                    + "). Feed can only be logged for today or earlier.");
        }
        long dayCount = ChronoUnit.DAYS.between(from, to) + 1;
        if (dayCount > properties.maxDays()) {
            throw new ToolUserException("The range is " + dayCount + " days; at most " + properties.maxDays()
                    + " days can be logged at once. Split it into smaller ranges.");
        }
        boolean allowOverlap = parseOverlap(overlap);
        if (bagsPerDay == null && kgPerDay == null) {
            throw new ToolUserException("Provide the daily quantity: either bagsPerDay or kgPerDay.");
        }
        if (bagsPerDay != null && kgPerDay != null) {
            throw new ToolUserException("Provide only one of bagsPerDay or kgPerDay, not both.");
        }
        double bagSize = bagSizeKg != null ? bagSizeKg : properties.defaultBagSizeKg();
        if (!(bagSize > 0)) {
            throw new ToolUserException("bagSizeKg must be greater than 0.");
        }
        double amount = bagsPerDay != null ? bagsPerDay : kgPerDay;
        if (!(amount > 0) || Double.isInfinite(amount)) {
            throw new ToolUserException("The daily quantity must be greater than 0.");
        }
        BigDecimal perDay = (bagsPerDay != null
                ? BigDecimal.valueOf(bagsPerDay).multiply(BigDecimal.valueOf(bagSize))
                : BigDecimal.valueOf(kgPerDay)).setScale(2, RoundingMode.HALF_UP);
        if (perDay.doubleValue() <= 0) {
            throw new ToolUserException("The daily quantity is too small to record (rounds to 0 kg).");
        }
        if (perDay.doubleValue() > properties.maxKgPerDay()) {
            throw new ToolUserException("That is " + fmt(perDay.doubleValue()) + " kg per day, above the limit of "
                    + fmt(properties.maxKgPerDay()) + " kg per day. Check the quantity and bag size.");
        }

        // ---- resolve what the user meant ----
        ResolvedBatch resolvedBatch = resolver.resolveBatch(batch);
        String resolvedFeed = resolver.resolveFeedType(feedType);

        FeedStock stock = farmData.feedStock().stream()
                .filter(s -> resolvedFeed.equals(s.getFeedType())).findFirst()
                .orElseThrow(() -> new ToolUserException("Feed type '" + resolvedFeed + "' is not in stock."));

        BigDecimal total = perDay.multiply(BigDecimal.valueOf(dayCount));
        double totalKg = total.doubleValue();
        double before = stock.getCurrentStock() == null ? 0.0 : stock.getCurrentStock();
        double after = round2(before - totalKg);
        boolean enoughStock = before >= totalKg;

        String tenantId = currentTenant();
        List<Conflict> conflicts = feedLogLookup.conflicts(resolvedBatch.id(), from, to);

        List<Row> rows = new ArrayList<>();
        List<BulkFeedLogRequest.Entry> entries = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            rows.add(new Row(d.toString(), perDay.doubleValue()));
            entries.add(new BulkFeedLogRequest.Entry(d, null, perDay.doubleValue()));
        }

        List<String> warnings = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (!enoughStock) {
            problems.add("Insufficient stock of " + resolvedFeed + ": " + fmt(before) + " kg available, "
                    + fmt(totalKg) + " kg needed (short by " + fmt(totalKg - before) + " kg).");
        } else if (stock.getThreshold() != null && after < stock.getThreshold()) {
            warnings.add("Stock of " + resolvedFeed + " will be low afterwards: " + fmt(after)
                    + " kg left (reorder threshold " + fmt(stock.getThreshold()) + " kg).");
        }
        if (!conflicts.isEmpty()) {
            String dates = String.join(", ", conflicts.stream().map(Conflict::date).distinct().toList());
            if (allowOverlap) {
                warnings.add("Feed logs already exist for this batch on " + dates
                        + ". These entries will be added on top of them.");
            } else {
                problems.add("Feed logs already exist for this batch on " + dates + ", which overlap the range. "
                        + "To avoid double counting nothing can be confirmed. Ask the user whether to choose a range "
                        + "without those dates, or to add on top (call again with overlap='add').");
            }
        }

        boolean ready = problems.isEmpty();
        String token = null;
        if (ready) {
            FeedingPlan plan = new FeedingPlan(resolvedBatch.id(), resolvedBatch.name(), resolvedFeed,
                    List.copyOf(entries), totalKg, allowOverlap);
            token = previewTokens.issue(tenantId, CurrentUser.subject(), CONFIRM_TOOL, plan);
        }

        String message = ready
                ? "Nothing has been saved. Show this plan to the user (batch, feed type, dates, kg per day, total, "
                + "and stock before/after) and ask for approval. Only if they clearly approve, call "
                + CONFIRM_TOOL + " with previewToken. The token expires in 10 minutes."
                : "Nothing has been saved and this plan cannot be confirmed. " + String.join(" ", problems);

        return new FeedingPreview(
                ready ? READY : BLOCKED, message,
                resolvedBatch.id(), resolvedBatch.name(), resolvedBatch.pondNames(), resolvedFeed,
                from.toString(), to.toString(), (int) dayCount,
                bagsPerDay, bagSize, perDay.doubleValue(), totalKg, round2(totalKg / bagSize),
                before, enoughStock ? after : null,
                rows, warnings, conflicts, token, ready ? 10 : null);
    }

    @Tool(name = "confirm_feeding",
          description = "Step 2 of 2: SAVES the feeding plan from preview_feeding. Call this ONLY after the user has "
                  + "clearly approved that exact plan in the current conversation. Needs the previewToken returned by "
                  + "preview_feeding. A token works once and expires after 10 minutes; if it fails, run "
                  + "preview_feeding again and ask the user again.")
    public FeedingResult confirmFeeding(
            @ToolParam(description = "The previewToken returned by preview_feeding") String previewToken) {
        String tenantId = currentTenant();
        String subject = CurrentUser.subject();
        FeedingPlan plan = previewTokens.consume(previewToken, tenantId, subject, CONFIRM_TOOL, FeedingPlan.class);

        LocalDate from = plan.entries().get(0).date();
        LocalDate to = plan.entries().get(plan.entries().size() - 1).date();

        if (!plan.allowOverlap()) {
            List<Conflict> conflicts = feedLogLookup.conflicts(plan.batchId(), from, to);
            if (!conflicts.isEmpty()) {
                throw new ToolUserException("Feed logs now exist for this batch on "
                        + String.join(", ", conflicts.stream().map(Conflict::date).distinct().toList())
                        + ", so nothing was saved. Run preview_feeding again.");
            }
        }

        BulkFeedLogResponse response;
        try {
            response = apiClient.bulkFeed(new BulkFeedLogRequest(plan.batchId(), plan.feedType(), plan.entries()));
        } catch (ToolUserException e) {
            log.warn("AUDIT feeding rejected tenant={} user={} batch={} totalKg={}: {}",
                    tenantId, subject, plan.batchId(), plan.totalKg(), e.getMessage());
            throw new ToolUserException(e.getMessage() + " Run preview_feeding again to retry.", e);
        }

        int created = response.createdIds() == null ? 0 : response.createdIds().size();
        log.info("AUDIT feeding saved tenant={} user={} batch={} feedType={} range={}..{} logs={} totalKg={} remainingKg={}",
                tenantId, subject, plan.batchId(), plan.feedType(), from, to, created,
                response.totalKg(), response.remainingStockKg());

        return new FeedingResult("SAVED",
                "Saved " + created + " feed logs for " + (plan.batchName() == null ? plan.batchId() : plan.batchName())
                        + " (" + plan.feedType() + ", " + from + " to " + to + "): " + fmt(response.totalKg())
                        + " kg deducted from stock, " + fmt(response.remainingStockKg()) + " kg left.",
                plan.batchName(), plan.feedType(), from.toString(), to.toString(), created,
                response.totalKg(), response.remainingStockKg());
    }

    private static String currentTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ToolUserException("No tenant is bound to this request.");
        }
        return tenantId;
    }

    private static LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ToolUserException(field + " is required (format YYYY-MM-DD).");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ToolUserException("'" + value + "' is not a valid " + field + ". Use the format YYYY-MM-DD.");
        }
    }

    /** @return true if existing logs may be overlapped ("add"), false to block (default). */
    private static boolean parseOverlap(String overlap) {
        if (overlap == null || overlap.isBlank() || "block".equalsIgnoreCase(overlap.trim())) {
            return false;
        }
        if ("add".equalsIgnoreCase(overlap.trim())) {
            return true;
        }
        if ("replace".equalsIgnoreCase(overlap.trim())) {
            throw new ToolUserException("overlap='replace' is not supported yet. Use 'block' or 'add'.");
        }
        throw new ToolUserException("overlap must be 'block' or 'add'.");
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String fmt(double v) {
        BigDecimal bd = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }
}
