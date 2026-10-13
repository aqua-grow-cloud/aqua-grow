package com.aquagrow.mcp.security;

import com.aquagrow.mcp.client.ToolUserException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** The authenticated caller of the current MCP request, taken from the validated JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    private static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new ToolUserException("No authenticated user for this request");
    }

    public static String subject() {
        return jwt().getSubject();
    }

    /** The raw bearer token, forwarded to aqua-grow-api so it applies the same tenant rules. */
    public static String bearerToken() {
        return jwt().getTokenValue();
    }
}
