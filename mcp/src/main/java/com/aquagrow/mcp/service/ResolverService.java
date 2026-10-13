package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.AquaGrowApiClient;
import com.aquagrow.mcp.client.FeedStockView;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.FishType;
import com.aquagrow.mcp.model.Pond;
import com.aquagrow.mcp.repository.BatchRepository;
import com.aquagrow.mcp.repository.FishTypeRepository;
import com.aquagrow.mcp.repository.PondRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final BatchRepository batchRepository;
    private final PondRepository pondRepository;
    private final FishTypeRepository fishTypeRepository;
    private final TenantFilterActivator tenantFilterActivator;
    private final AquaGrowApiClient apiClient;

    public record ResolvedBatch(String id, String name, List<String> pondNames, LocalDate stockingDate) {
    }

    /** More than one thing matches; the message lists them so the user can choose. */
    public static class AmbiguousReferenceException extends ToolUserException {
        public AmbiguousReferenceException(String message) {
            super(message);
        }
    }

    // ---- batches ----

    @Transactional(readOnly = true)
    public ResolvedBatch resolveBatch(String reference) {
        String raw = reference == null ? "" : reference.trim();
        String needle = normalize(raw);
        if (needle.isEmpty()) {
            throw new ToolUserException("A batch is required: give a batch name, batch id or pond name.");
        }

        String tenantId = tenantFilterActivator.activate();
        List<Batch> batches = batchRepository.findByTenantId(tenantId);
        Map<String, Pond> pondsById = pondRepository.findByTenantId(tenantId).stream()
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
            List<Batch> active = matches.stream().filter(ResolverService::isActive).toList();
            if (active.size() == 1) {
                return toResolved(active.get(0), pondsById);
            }
            if (active.size() > 1) {
                throw new AmbiguousReferenceException("More than one active batch matches '" + raw
                        + "'. Ask the user which one they mean, then use its id:\n"
                        + describeBatches(active, pondsById));
            }
            matchedOnlyCompleted = true;
        }

        if (matchedOnlyCompleted) {
            throw new ToolUserException("Batch '" + raw
                    + "' is already completed (all its fish are harvested), so it cannot be used here.");
        }
        List<Batch> allActive = batches.stream().filter(ResolverService::isActive).toList();
        throw new ToolUserException("No active batch matches '" + raw + "'. "
                + (allActive.isEmpty()
                ? "There are no active batches."
                : "Active batches:\n" + describeBatches(allActive, pondsById)));
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
        return new ResolvedBatch(batch.getId(), batch.getName(), pondNames, batch.getStockingDate());
    }

    private String describeBatches(List<Batch> batches, Map<String, Pond> pondsById) {
        Map<String, String> fishTypeNames = fishTypeRepository.findByTenantId(tenantFilterActivator.activate()).stream()
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

    // ---- feed types ----

    /**
     * Resolves a feed type against the stock the caller can actually use. Stock is
     * read through aqua-grow-api because a sub-tenant's feed comes from its parent's
     * global stock, which a tenant-filtered database read would not return.
     */
    public String resolveFeedType(String reference) {
        String raw = reference == null ? "" : reference.trim();
        String needle = normalize(raw);
        if (needle.isEmpty()) {
            throw new ToolUserException("A feed type is required.");
        }

        List<String> types = apiClient.getFeedStock().stream()
                .map(FeedStockView::feedType).filter(Objects::nonNull).distinct().toList();
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
