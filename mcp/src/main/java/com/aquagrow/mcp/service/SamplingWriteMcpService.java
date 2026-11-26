package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.AquaGrowApiClient;
import com.aquagrow.mcp.client.SampleLogPayload;
import com.aquagrow.mcp.client.SampleLogResult;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.confirm.PreviewTokenService;
import com.aquagrow.mcp.security.CurrentUser;
import com.aquagrow.mcp.security.TenantContext;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import com.aquagrow.mcp.service.ResolverService.ResolvedFishType;
import com.aquagrow.mcp.service.SampleLogLookup.Context;
import com.aquagrow.mcp.service.SampleLogLookup.Existing;
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

/**
 * Growth sampling writes behind a preview, then explicit confirmation.
 *
 * {@code preview_sample} computes the average weight and checks the entry, changing
 * nothing; {@code confirm_sample} accepts only the token a preview issued and saves through
 * aqua-grow-api so its validation and the sampling stock event apply.
 * All weights are in grams, lengths in centimetres.
 */
@Service
@Slf4j
public class SamplingWriteMcpService {

    static final String CONFIRM_TOOL = "confirm_sample";
    private static final int MAX_SAMPLE_COUNT = 10_000;
    private static final double MAX_LENGTH_CM = 500;
    private static final String READY = "READY_TO_CONFIRM";
    private static final String BLOCKED = "BLOCKED";

    public record SamplePreview(
            String status,
            String message,
            String batchId,
            String batchName,
            String fishTypeId,
            String fishType,
            String date,
            int sampleCount,
            double totalSampleWeightGrams,
            double averageWeightGrams,
            Double averageLengthCm,
            Double survivalEstimatePercent,
            String stockEventPond,
            String previousSampleDate,
            Double previousAverageWeightGrams,
            Double growthGramsPerDay,
            List<String> warnings,
            List<Existing> duplicates,
            String previewToken,
            Integer expiresInMinutes) {
    }

    public record SampleResult(
            String status,
            String message,
            String sampleLogId,
            String batchName,
            String fishType,
            String date,
            int sampleCount,
            double averageWeightGrams) {
    }

    /** What a preview stores server-side and a confirm later executes. */
    public record SamplePlan(
            String batchId,
            String batchName,
            String fishTypeId,
            String fishTypeName,
            LocalDate date,
            int sampleCount,
            double totalSampleWeight,
            double averageWeight,
            Double averageLength,
            Double survivalEstimate,
            boolean allowDuplicate) {
    }

    private final ResolverService resolver;
    private final AquaGrowApiClient apiClient;
    private final SampleLogLookup sampleLogLookup;
    private final PreviewTokenService previewTokens;
    private final Clock clock;

    public SamplingWriteMcpService(ResolverService resolver, AquaGrowApiClient apiClient,
                                   SampleLogLookup sampleLogLookup, PreviewTokenService previewTokens, Clock clock) {
        this.resolver = resolver;
        this.apiClient = apiClient;
        this.sampleLogLookup = sampleLogLookup;
        this.previewTokens = previewTokens;
        this.clock = clock;
    }

    @Tool(name = "preview_sample",
          description = "Step 1 of 2 for logging a fish growth sample. Checks the entry and shows what would be "
                  + "recorded, but DOES NOT SAVE ANYTHING. The average weight is worked out from sampleCount and "
                  + "totalSampleWeightGrams. Weights are in GRAMS (convert kg to grams: 2 kg = 2000 g). Never guess "
                  + "the batch, fish type, count or weights: ask the user if anything is missing or ambiguous. "
                  + "Show the returned entry and warnings to the user and wait for their explicit approval before "
                  + "calling confirm_sample.")
    public SamplePreview previewSample(
            @ToolParam(description = "Batch name, batch id, or pond name (e.g. 'Pond 3')") String batch,
            @ToolParam(description = "Sampling date, YYYY-MM-DD") String date,
            @ToolParam(description = "Number of fish weighed (pieces)") Integer sampleCount,
            @ToolParam(description = "Total weight of all sampled fish together, in GRAMS") Double totalSampleWeightGrams,
            @ToolParam(description = "Fish type, only needed when the batch has more than one", required = false) String fishType,
            @ToolParam(description = "Average length in cm, if measured", required = false) Double averageLengthCm,
            @ToolParam(description = "Survival estimate in percent (0-100), if known", required = false) Double survivalEstimatePercent,
            @ToolParam(description = "What to do if a sample of the same fish type already exists on that date: "
                    + "'block' (default) or 'add' (only if the user explicitly wants another entry).", required = false) String overlap) {

        LocalDate sampleDate = parseDate(date);
        LocalDate today = LocalDate.now(clock);
        if (sampleDate.isAfter(today)) {
            throw new ToolUserException("The sampling date " + sampleDate + " is in the future (today is " + today
                    + "). Samples can only be logged for today or earlier.");
        }
        boolean allowDuplicate = parseOverlap(overlap);
        if (sampleCount == null || sampleCount <= 0) {
            throw new ToolUserException("sampleCount (number of fish weighed) must be greater than 0.");
        }
        if (sampleCount > MAX_SAMPLE_COUNT) {
            throw new ToolUserException("sampleCount " + sampleCount + " looks too large (limit " + MAX_SAMPLE_COUNT
                    + "). Check the number.");
        }
        if (totalSampleWeightGrams == null || !(totalSampleWeightGrams > 0) || Double.isInfinite(totalSampleWeightGrams)) {
            throw new ToolUserException("totalSampleWeightGrams must be greater than 0 (weight in grams).");
        }
        if (averageLengthCm != null && (!(averageLengthCm > 0) || averageLengthCm > MAX_LENGTH_CM)) {
            throw new ToolUserException("The average length must be between 0 and " + (int) MAX_LENGTH_CM + " cm.");
        }
        if (survivalEstimatePercent != null && (!(survivalEstimatePercent >= 0) || survivalEstimatePercent > 100)) {
            throw new ToolUserException("The survival estimate must be between 0 and 100 percent.");
        }

        double average = BigDecimal.valueOf(totalSampleWeightGrams)
                .divide(BigDecimal.valueOf(sampleCount), 2, RoundingMode.HALF_UP).doubleValue();
        if (!(average > 0)) {
            throw new ToolUserException("The average weight rounds to 0 g; check the count and the total weight (grams).");
        }

        ResolvedBatch resolvedBatch = resolver.resolveBatch(batch);
        ResolvedFishType fish = resolver.resolveFishType(resolvedBatch.id(), fishType);
        String tenantId = currentTenant();
        Context context = sampleLogLookup.context(resolvedBatch.id(), fish.id(), sampleDate);

        List<String> warnings = new ArrayList<>();
        List<String> problems = new ArrayList<>();

        if (!context.sameDay().isEmpty()) {
            if (allowDuplicate) {
                warnings.add("A sample of " + fish.name() + " already exists for this batch on " + sampleDate
                        + ". This entry will be added as an extra sample.");
            } else {
                problems.add("A sample of " + fish.name() + " already exists for this batch on " + sampleDate
                        + ", which would overlap. Ask the user whether this is a different sample (then call again with "
                        + "overlap='add') or a duplicate.");
            }
        }

        String previousDate = null;
        Double previousAverage = null;
        Double growthPerDay = null;
        Existing previous = context.previous();
        if (previous != null && previous.averageWeight() != null) {
            previousDate = previous.date();
            previousAverage = previous.averageWeight();
            long days = ChronoUnit.DAYS.between(LocalDate.parse(previous.date()), sampleDate);
            if (days > 0) {
                growthPerDay = BigDecimal.valueOf(average - previousAverage)
                        .divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP).doubleValue();
            }
            double ratio = average / previousAverage;
            if (ratio < 0.5 || ratio > 3) {
                warnings.add("This average weight (" + fmt(average) + " g) differs a lot from the previous sample ("
                        + fmt(previousAverage) + " g on " + previous.date() + "). Check the count and that the total "
                        + "weight is in grams, not kilograms.");
            }
        }
        if (averageLengthCm == null) {
            warnings.add("No average length was given, so it will be left empty.");
        }
        if (survivalEstimatePercent == null) {
            warnings.add("No survival estimate was given, so it will be left empty.");
        }

        String stockEventPond = fish.pondNames().size() == 1
                ? fish.pondNames().get(0)
                : "N/A (this fish type is in several ponds of the batch)";

        boolean ready = problems.isEmpty();
        String token = null;
        if (ready) {
            SamplePlan plan = new SamplePlan(resolvedBatch.id(), resolvedBatch.name(), fish.id(), fish.name(),
                    sampleDate, sampleCount, totalSampleWeightGrams, average, averageLengthCm,
                    survivalEstimatePercent, allowDuplicate);
            token = previewTokens.issue(tenantId, CurrentUser.subject(), CONFIRM_TOOL, plan);
        }

        String message = ready
                ? "Nothing has been saved. Show this sample (batch, fish type, date, count, total and average weight, "
                + "and the warnings) to the user and ask for approval. Only if they clearly approve, call "
                + CONFIRM_TOOL + " with previewToken. It also records a sampling event for the batch (no change to the "
                + "fish count). The token expires in 10 minutes."
                : "Nothing has been saved and this entry cannot be confirmed. " + String.join(" ", problems);

        return new SamplePreview(ready ? READY : BLOCKED, message,
                resolvedBatch.id(), resolvedBatch.name(), fish.id(), fish.name(), sampleDate.toString(),
                sampleCount, totalSampleWeightGrams, average, averageLengthCm, survivalEstimatePercent,
                stockEventPond, previousDate, previousAverage, growthPerDay,
                warnings, context.sameDay(), token, ready ? 10 : null);
    }

    @Tool(name = "confirm_sample",
          description = "Step 2 of 2: SAVES the sample from preview_sample. Call this ONLY after the user has clearly "
                  + "approved that exact entry in the current conversation. Needs the previewToken returned by "
                  + "preview_sample. A token works once and expires after 10 minutes; if it fails, run preview_sample "
                  + "again and ask the user again.")
    public SampleResult confirmSample(
            @ToolParam(description = "The previewToken returned by preview_sample") String previewToken) {
        String tenantId = currentTenant();
        String subject = CurrentUser.subject();
        SamplePlan plan = previewTokens.consume(previewToken, tenantId, subject, CONFIRM_TOOL, SamplePlan.class);

        if (!plan.allowDuplicate()
                && !sampleLogLookup.context(plan.batchId(), plan.fishTypeId(), plan.date()).sameDay().isEmpty()) {
            throw new ToolUserException("A sample of " + plan.fishTypeName() + " now exists for this batch on "
                    + plan.date() + ", so nothing was saved. Run preview_sample again.");
        }

        SampleLogResult saved;
        try {
            saved = apiClient.createSample(new SampleLogPayload(plan.batchId(), plan.fishTypeId(), plan.date(),
                    plan.sampleCount(), plan.totalSampleWeight(), plan.averageWeight(), plan.averageLength(),
                    plan.survivalEstimate()));
        } catch (ToolUserException e) {
            log.warn("AUDIT sample rejected tenant={} user={} batch={}: {}", tenantId, subject, plan.batchId(), e.getMessage());
            throw new ToolUserException(e.getMessage() + " Run preview_sample again to retry.", e);
        }

        log.info("AUDIT sample saved tenant={} user={} batch={} fishType={} date={} count={} avgG={} id={}",
                tenantId, subject, plan.batchId(), plan.fishTypeId(), plan.date(), plan.sampleCount(),
                plan.averageWeight(), saved.id());

        return new SampleResult("SAVED",
                "Saved the sample for " + (plan.batchName() == null ? plan.batchId() : plan.batchName()) + " ("
                        + plan.fishTypeName() + ", " + plan.date() + "): " + plan.sampleCount() + " fish, average "
                        + fmt(plan.averageWeight()) + " g.",
                saved.id(), plan.batchName(), plan.fishTypeName(), plan.date().toString(),
                plan.sampleCount(), plan.averageWeight());
    }

    private static String currentTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ToolUserException("No tenant is bound to this request.");
        }
        return tenantId;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new ToolUserException("date is required (format YYYY-MM-DD).");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ToolUserException("'" + value + "' is not a valid date. Use the format YYYY-MM-DD.");
        }
    }

    /** @return true if a same-day sample may be added anyway ("add"), false to block (default). */
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

    private static String fmt(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
