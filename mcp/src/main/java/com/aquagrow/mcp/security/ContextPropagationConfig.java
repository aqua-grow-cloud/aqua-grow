package com.aquagrow.mcp.security;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Makes the caller's identity follow a tool call onto the worker thread the MCP server runs it on.
 *
 * Without this, tools see no tenant ("No tenant bound to the current request context") because the
 * MCP SDK (0.10) executes synchronous handlers on Reactor's bounded-elastic scheduler. With it,
 * Reactor copies {@link CallerContext} onto the worker thread for the call and clears it afterwards,
 * so a pooled thread never carries one caller's identity into the next call.
 */
@Configuration
public class ContextPropagationConfig {

    static final String KEY = "aquagrow.caller";

    @PostConstruct
    public void enable() {
        install();
    }

    /** Idempotent; also used by tests. */
    public static void install() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                KEY, CallerContext::capture, CallerContext::restore, CallerContext::reset);
        Hooks.enableAutomaticContextPropagation();
    }
}
