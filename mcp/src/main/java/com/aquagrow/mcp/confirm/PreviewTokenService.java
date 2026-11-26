package com.aquagrow.mcp.confirm;

import com.aquagrow.mcp.client.ToolUserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues single-use, short-lived tokens for the preview/confirm write flow.
 *
 * A preview stores its exact payload server-side and returns an opaque random
 * token. {@code confirm_*} tools can only act on a payload that a preview of
 * the same tool, tenant and user produced, within the TTL, and only once.
 * Because the payload never leaves the server, there is nothing for the model
 * to tamper with; an unknown token is simply rejected.
 *
 * In-memory: tokens are lost on restart and not shared between replicas, which
 * is acceptable for a single-replica deployment (a lost token just means the
 * user previews again).
 */
@Component
public class PreviewTokenService {

    static final int MAX_LIVE_TOKENS = 1000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private record Entry(String tenantId, String subject, String toolName, Object payload, Instant expiresAt) {
    }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public PreviewTokenService(@Value("${aquagrow.mcp.preview-ttl:PT10M}") Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    PreviewTokenService(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    public String issue(String tenantId, String subject, String toolName, Object payload) {
        require(tenantId, "tenant");
        require(subject, "user");
        purgeExpired();
        if (tokens.size() >= MAX_LIVE_TOKENS) {
            throw new ToolUserException("Too many pending previews. Please try again in a few minutes.");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, new Entry(tenantId, subject, toolName, payload, clock.instant().plus(ttl)));
        return token;
    }

    /**
     * Returns the previewed payload and invalidates the token. A token presented by a
     * different tenant, user or tool is rejected without being consumed.
     */
    public <T> T consume(String token, String tenantId, String subject, String toolName, Class<T> payloadType) {
        Entry entry = token == null ? null : tokens.get(token);
        if (entry == null) {
            throw invalid();
        }
        if (!clock.instant().isBefore(entry.expiresAt())) {
            tokens.remove(token, entry);
            throw new ToolUserException("This preview has expired. Please run the preview again.");
        }
        if (!entry.tenantId().equals(tenantId) || !entry.subject().equals(subject)
                || !entry.toolName().equals(toolName) || !payloadType.isInstance(entry.payload())) {
            throw invalid();
        }
        if (!tokens.remove(token, entry)) {
            throw invalid();
        }
        return payloadType.cast(entry.payload());
    }

    int liveTokenCount() {
        purgeExpired();
        return tokens.size();
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        tokens.entrySet().removeIf(e -> !now.isBefore(e.getValue().expiresAt()));
    }

    private static void require(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new ToolUserException("Cannot create a preview without an authenticated " + what);
        }
    }

    private static ToolUserException invalid() {
        return new ToolUserException("This preview is not valid (unknown, already used, or created by someone else). "
                + "Please run the preview again.");
    }
}
