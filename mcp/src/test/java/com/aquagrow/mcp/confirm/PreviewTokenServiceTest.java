package com.aquagrow.mcp.confirm;

import com.aquagrow.mcp.client.ToolUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreviewTokenServiceTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-13T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private PreviewTokenService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new PreviewTokenService(Duration.ofMinutes(10), clock);
    }

    @Test
    void consumeReturnsTheStoredPayload() {
        String token = service.issue("t1", "u1", "confirm_feeding", "payload");

        String payload = service.consume(token, "t1", "u1", "confirm_feeding", String.class);

        assertThat(payload).isEqualTo("payload");
    }

    @Test
    void tokenIsSingleUse() {
        String token = service.issue("t1", "u1", "confirm_feeding", "payload");
        service.consume(token, "t1", "u1", "confirm_feeding", String.class);

        assertThatThrownBy(() -> service.consume(token, "t1", "u1", "confirm_feeding", String.class))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = service.issue("t1", "u1", "confirm_feeding", "payload");
        clock.advance(Duration.ofMinutes(10));

        assertThatThrownBy(() -> service.consume(token, "t1", "u1", "confirm_feeding", String.class))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void tokenStillValidJustBeforeExpiry() {
        String token = service.issue("t1", "u1", "confirm_feeding", "payload");
        clock.advance(Duration.ofMinutes(10).minusSeconds(1));

        assertThat(service.consume(token, "t1", "u1", "confirm_feeding", String.class)).isEqualTo("payload");
    }

    @Test
    void differentTenantUserOrToolIsRejectedAndTokenSurvives() {
        String token = service.issue("t1", "u1", "confirm_feeding", "payload");

        assertThatThrownBy(() -> service.consume(token, "other-tenant", "u1", "confirm_feeding", String.class))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.consume(token, "t1", "other-user", "confirm_feeding", String.class))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.consume(token, "t1", "u1", "confirm_sample", String.class))
                .isInstanceOf(ToolUserException.class);

        // The rightful owner can still confirm after the failed attempts.
        assertThat(service.consume(token, "t1", "u1", "confirm_feeding", String.class)).isEqualTo("payload");
    }

    @Test
    void wrongPayloadTypeIsRejected() {
        String token = service.issue("t1", "u1", "confirm_feeding", "payload");

        assertThatThrownBy(() -> service.consume(token, "t1", "u1", "confirm_feeding", Integer.class))
                .isInstanceOf(ToolUserException.class);
    }

    @Test
    void unknownNullOrTamperedTokenIsRejected() {
        String token = service.issue("t1", "u1", "confirm_feeding", "payload");

        assertThatThrownBy(() -> service.consume("made-up", "t1", "u1", "confirm_feeding", String.class))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.consume(null, "t1", "u1", "confirm_feeding", String.class))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.consume(token + "x", "t1", "u1", "confirm_feeding", String.class))
                .isInstanceOf(ToolUserException.class);
    }

    @Test
    void tokensAreUniqueAndUnguessableInShape() {
        String a = service.issue("t1", "u1", "confirm_feeding", "p");
        String b = service.issue("t1", "u1", "confirm_feeding", "p");

        assertThat(a).isNotEqualTo(b);
        assertThat(a).hasSizeGreaterThanOrEqualTo(40).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void issueRequiresTenantAndUser() {
        assertThatThrownBy(() -> service.issue(null, "u1", "confirm_feeding", "p"))
                .isInstanceOf(ToolUserException.class);
        assertThatThrownBy(() -> service.issue("t1", " ", "confirm_feeding", "p"))
                .isInstanceOf(ToolUserException.class);
    }

    @Test
    void expiredTokensArePurgedAndCapacityIsBounded() {
        for (int i = 0; i < PreviewTokenService.MAX_LIVE_TOKENS; i++) {
            service.issue("t1", "u1", "confirm_feeding", "p");
        }
        assertThatThrownBy(() -> service.issue("t1", "u1", "confirm_feeding", "p"))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("Too many");

        clock.advance(Duration.ofMinutes(11));

        service.issue("t1", "u1", "confirm_feeding", "p");
        assertThat(service.liveTokenCount()).isEqualTo(1);
    }
}
