package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.FeedStock;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.FishType;
import com.aquagrow.mcp.model.Pond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Turns what a person says ("pond 3", "the September tilapia", "king fish feed")
 * into the exact ids and spellings the write tools need. It never guesses: an
 * ambiguous or unknown reference produces an error that lists the candidates so
 * the model can ask the user.
 *
 * Batches are matched, in order, by id, by exact name, by pond (id or name), and
 * finally by partial name or pond name. Only active batches can be returned.
 */
@Service
@RequiredArgsConstructor
public class ResolverService {

    private static final int MAX_LISTED = 10;

    private final FarmDataClient farmData;

    public record ResolvedBatch(String id, String name, List<String> pondNames, LocalDate stockingDate, String status) {

        /** Convenience for callers that only deal with active batches. */
        public ResolvedBatch(String id, String name, List<String> pondNames, LocalDate stockingDate) {
            this(id, name, pondNames, stockingDate, "Active");
        }
    }

    public record ResolvedFishType(String id, String name, List<String> pondNames) {
    }

    /** More than one thing matches; the message lists them so the user can choose. */
    public static class AmbiguousReferenceException extends ToolUserException {
        public AmbiguousReferenceException(String message) {
            super(message);
        }
    }

    // ---- batches ----

    /** Resolves to a single ACTIVE batch (for logging feed or samples). */
    public ResolvedBatch resolveBatch(String reference) {
        return resolve(reference, true);
    }

    /** Resolves to a single batch of any status, completed ones included (for reading and analysis). */
    public ResolvedBatch resolveAnyBatch(String reference) {
        return resolve(reference, false);
    }

    private ResolvedBatch resolve(String reference, boolean activeOnly) {
        String raw = reference == null ? "" : reference.trim();
        String needle = normalize(raw);
        if (needle.isEmpty()) {
            throw new ToolUserException("A batch is required: give a batch name, batch id or pond name.");
        }

        List<Batch> batches = farmData.batches();
        Map<String, Pond> pondsById = farmData.ponds().stream()
                .collect(Collectors.toMap(Pond::getId, p -> p, (a, b) -> a));

        List<Predicate<Batch>> tiers = List.of(
                b -> raw.equalsIgnoreCase(b.getId()),
                b -> normalize(b.getName()).equals(needle),
                b -> pondsOf(b, pondsById).stream()
                        .anyMatch(p -> raw.equalsIgnoreCase(p.getId()) || normalize(p.getName()).equals(needle)),
                b -> normalize(b.getName()).contains(needle)
                        || pondsOf(b, pondsById).stream().anyMatch(p -> normalize(p.getName()).contains(needle)));

        boolean matchedOnlyCompleted = false;
        for (Predicate<Batch> tier : tiers) {
            List<Batch> matches = batches.stream().filter(tier).toList();
            if (matches.isEmpty()) {
                continue;
            }
            List<Batch> candidates = activeOnly ? matches.stream().filter(ResolverService::isActive).toList() : matches;
            if (candidates.size() == 1) {
                return toResolved(candidates.get(0), pondsById);
            }
            if (candidates.size() > 1) {
                throw new AmbiguousReferenceException("More than one " + (activeOnly ? "active " : "") + "batch matches '"
                        + raw + "'. Ask the user which one they mean, then use its id:\n"
                        + describeBatches(candidates, pondsById));
            }
            matchedOnlyCompleted = true;
        }

        if (matchedOnlyCompleted) {
            throw new ToolUserException("Batch '" + raw
                    + "' is already completed (all its fish are harvested), so it cannot be used here.");
        }
        List<Batch> listed = activeOnly ? batches.stream().filter(ResolverService::isActive).toList() : batches;
        String kind = activeOnly ? "active batch" : "batch";
        throw new ToolUserException("No " + kind + " matches '" + raw + "'. "
                + (listed.isEmpty()
                ? "There are no " + kind + "es."
                : (activeOnly ? "Active batches" : "Batches") + ":\n" + describeBatches(listed, pondsById)));
    }

    private static boolean isActive(Batch batch) {
        return "Active".equals(batch.getStatus());
    }

    private static List<Pond> pondsOf(Batch batch, Map<String, Pond> pondsById) {
        Set<String> ids = new LinkedHashSet<>();
        if (batch.getFishSeeds() != null) {
            for (FishSeed seed : batch.getFishSeeds()) {
                if (seed.getPondId() != null) {
                    ids.add(seed.getPondId());
                }
            }
        }
        pondsById.values().stream()
                .filter(p -> batch.getId() != null && batch.getId().equals(p.getCurrentBatchId()))
                .forEach(p -> ids.add(p.getId()));
        return ids.stream().map(pondsById::get).filter(Objects::nonNull).toList();
    }

    private ResolvedBatch toResolved(Batch batch, Map<String, Pond> pondsById) {
        List<String> pondNames = pondsOf(batch, pondsById).stream().map(Pond::getName).toList();
        return new ResolvedBatch(batch.getId(), batch.getName(), pondNames, batch.getStockingDate(), batch.getStatus());
    }

    private String describeBatches(List<Batch> batches, Map<String, Pond> pondsById) {
        Map<String, String> fishTypeNames = farmData.fishTypes().stream()
                .collect(Collectors.toMap(FishType::getId, FishType::getName, (a, b) -> a));
        StringBuilder out = new StringBuilder();
        batches.stream().limit(MAX_LISTED).forEach(b -> {
            List<String> parts = new java.util.ArrayList<>(pondsOf(b, pondsById).stream().map(Pond::getName).toList());
            if (b.getFishSeeds() != null) {
                b.getFishSeeds().stream().map(FishSeed::getFishTypeId).filter(Objects::nonNull).distinct()
                        .map(id -> fishTypeNames.getOrDefault(id, null)).filter(Objects::nonNull)
                        .forEach(parts::add);
            }
            if (b.getStockingDate() != null) {
                parts.add("stocked " + b.getStockingDate());
            }
            parts.add(b.getStatus());
            out.append("- [").append(b.getId()).append("] ")
                    .append(b.getName() == null || b.getName().isBlank() ? "(unnamed)" : b.getName())
                    .append(parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")")
                    .append('\n');
        });
        if (batches.size() > MAX_LISTED) {
            out.append("...and ").append(batches.size() - MAX_LISTED).append(" more\n");
        }
        return out.toString().stripTrailing();
    }

    // ---- fish type within a batch ----

    /**
     * Picks the fish type being worked on within a batch. A batch with a single fish type
     * needs no reference; with several, the user must say which. The ponds returned are
     * those holding that fish type in this batch (what a sampling stock event is tied to).
     */
    public ResolvedFishType resolveFishType(String batchId, String reference) {
        Batch batch = farmData.batch(batchId);   // the client hides batches that belong to another farm

        Map<String, String> namesById = farmData.fishTypes().stream()
                .collect(Collectors.toMap(FishType::getId, FishType::getName, (a, b) -> a));
        List<FishSeed> seeds = batch.getFishSeeds() == null ? List.of() : batch.getFishSeeds();
        List<String> typeIds = seeds.stream().map(FishSeed::getFishTypeId).filter(Objects::nonNull).distinct().toList();
        if (typeIds.isEmpty()) {
            throw new ToolUserException("This batch has no fish types recorded, so it cannot be used here.");
        }

        String raw = reference == null ? "" : reference.trim();
        String needle = normalize(raw);
        String chosen;
        if (needle.isEmpty()) {
            if (typeIds.size() > 1) {
                throw new AmbiguousReferenceException("This batch has several fish types: "
                        + describeFishTypes(typeIds, namesById) + ". Ask the user which one they mean.");
            }
            chosen = typeIds.get(0);
        } else {
            List<String> exact = typeIds.stream()
                    .filter(id -> raw.equalsIgnoreCase(id) || normalize(namesById.get(id)).equals(needle)).toList();
            List<String> matches = exact.isEmpty()
                    ? typeIds.stream().filter(id -> normalize(namesById.get(id)).contains(needle)).toList()
                    : exact;
            if (matches.size() > 1) {
                throw new AmbiguousReferenceException("More than one fish type in this batch matches '" + raw + "': "
                        + describeFishTypes(matches, namesById) + ". Ask the user which one they mean.");
            }
            if (matches.isEmpty()) {
                throw new ToolUserException("No fish type matches '" + raw + "' in this batch. Fish types in this batch: "
                        + describeFishTypes(typeIds, namesById));
            }
            chosen = matches.get(0);
        }

        Map<String, Pond> pondsById = farmData.ponds().stream()
                .collect(Collectors.toMap(Pond::getId, p -> p, (a, b) -> a));
        List<String> pondNames = seeds.stream()
                .filter(sd -> chosen.equals(sd.getFishTypeId())).map(FishSeed::getPondId)
                .filter(Objects::nonNull).distinct().map(pondsById::get).filter(Objects::nonNull)
                .map(Pond::getName).toList();
        return new ResolvedFishType(chosen, namesById.getOrDefault(chosen, chosen), pondNames);
    }

    private static String describeFishTypes(List<String> ids, Map<String, String> namesById) {
        return ids.stream().map(id -> namesById.getOrDefault(id, id)).collect(Collectors.joining(", "));
    }

    // ---- feed types ----

    /**
     * Resolves a feed type against the stock the caller can actually use. The API
     * returns a sub-tenant's parent farm's global stock, which is what a sub-tenant can actually use.
     */
    public String resolveFeedType(String reference) {
        String raw = reference == null ? "" : reference.trim();
        String needle = normalize(raw);
        if (needle.isEmpty()) {
            throw new ToolUserException("A feed type is required.");
        }

        List<String> types = farmData.feedStock().stream()
                .map(FeedStock::getFeedType).filter(Objects::nonNull).distinct().toList();
        if (types.isEmpty()) {
            throw new ToolUserException("There is no feed stock set up, so no feed type can be used.");
        }

        List<String> exact = types.stream().filter(t -> normalize(t).equals(needle)).toList();
        if (exact.size() == 1) {
            return exact.get(0);
        }
        List<String> partial = types.stream().filter(t -> normalize(t).contains(needle)).toList();
        if (partial.size() == 1) {
            return partial.get(0);
        }
        if (partial.size() > 1) {
            throw new AmbiguousReferenceException("More than one feed type matches '" + raw
                    + "': " + String.join(", ", partial) + ". Ask the user which one they mean.");
        }
        throw new ToolUserException("No feed type matches '" + raw + "'. Available feed types: "
                + String.join(", ", types));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
