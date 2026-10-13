package com.aquagrow.mcp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Answers an unauthenticated request with the standard Bearer 401 and tells the client where the
 * OAuth protected-resource metadata lives (RFC 9728, required by the MCP authorization spec).
 * That pointer is how Claude's connector discovers the Keycloak login for this server.
 */
public class ResourceMetadataEntryPoint implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
    private final String metadataUrl;

    public ResourceMetadataEntryPoint(String resourceUri) {
        String base = resourceUri.endsWith("/") ? resourceUri.substring(0, resourceUri.length() - 1) : resourceUri;
        this.metadataUrl = base + "/.well-known/oauth-protected-resource";
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        delegate.commence(request, response, authException);
        String challenge = response.getHeader(HttpHeaders.WWW_AUTHENTICATE);
        String param = "resource_metadata=\"" + metadataUrl + "\"";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                challenge == null || challenge.isBlank() || "Bearer".equals(challenge.trim())
                        ? "Bearer " + param
                        : challenge + ", " + param);
    }
}
