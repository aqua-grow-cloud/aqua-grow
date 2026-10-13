package com.aquagrow.mcp.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces how the MCP server runs a synchronous tool: the HTTP request thread blocks on a Mono
 * whose work executes on the bounded-elastic scheduler.
 */
class ContextPropagationTest {

    @BeforeAll
    static void enable() {
        ContextPropagationConfig.install();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String subject, String tenant, String parent) {
        Jwt jwt = Jwt.withTokenValue("tok-" + subject).header("alg", "none").subject(subject)
                .claim("tenant_id", tenant).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        TenantContext.setTenantId(tenant);
        if (parent != null) {
            TenantContext.setParentTenantId(parent);
        }
    }

    private static String[] runOnWorker() {
        return Mono.fromCallable(() -> new String[]{
                        TenantContext.getTenantId(),
                        TenantContext.getParentTenantId(),
                        SecurityContextHolder.getContext().getAuthentication() == null
                                ? null : CurrentUser.subject(),
                        Thread.currentThread().getName()})
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    @Test
    void tenantParentAndLoginReachTheWorkerThread() {
        loginAs("owner", "farm-1", "root-farm");

        String[] seen = runOnWorker();

        assertThat(seen[3]).as("ran on a different thread").isNotEqualTo(Thread.currentThread().getName());
        assertThat(seen[0]).isEqualTo("farm-1");
        assertThat(seen[1]).isEqualTo("root-farm");
        assertThat(seen[2]).isEqualTo("owner");
    }

    @Test
    void workerThreadsDoNotKeepTheCallersIdentityAfterwards() {
        loginAs("owner", "farm-1", null);
        runOnWorker();
        TenantContext.clear();
        SecurityContextHolder.clearContext();

        String[] later = runOnWorker();   // an unauthenticated call that may reuse the same pooled thread

        assertThat(later[0]).isNull();
        assertThat(later[1]).isNull();
        assertThat(later[2]).isNull();
    }

    @Test
    void alternatingCallersNeverSeeEachOthersIdentity() {
        for (int i = 0; i < 300; i++) {
            String tenant = i % 2 == 0 ? "farm-A" : "farm-B";
            TenantContext.clear();
            SecurityContextHolder.clearContext();
            loginAs("user-" + tenant, tenant, null);

            String[] seen = runOnWorker();

            assertThat(seen[0]).as("call %d", i).isEqualTo(tenant);
            assertThat(seen[2]).as("call %d", i).isEqualTo("user-" + tenant);
        }
    }

    @Test
    void concurrentCallersStayIsolated() throws Exception {
        AtomicReference<String> failure = new AtomicReference<>();
        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            String tenant = "farm-" + t;
            threads[t] = new Thread(() -> {
                loginAs("user-" + tenant, tenant, null);
                for (int i = 0; i < 100; i++) {
                    String[] seen = runOnWorker();
                    if (!tenant.equals(seen[0]) || !("user-" + tenant).equals(seen[2])) {
                        failure.compareAndSet(null, tenant + " saw " + seen[0] + "/" + seen[2]);
                    }
                }
                TenantContext.clear();
                SecurityContextHolder.clearContext();
            });
            threads[t].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertThat(failure.get()).isNull();
    }

    @Test
    void noCallerMeansNoIdentityOnTheWorker() {
        String[] seen = runOnWorker();

        assertThat(seen[0]).isNull();
        assertThat(seen[2]).isNull();
    }
}
