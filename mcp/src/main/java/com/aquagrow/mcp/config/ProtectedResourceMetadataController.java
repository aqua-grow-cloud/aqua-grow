package com.aquagrow.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes the OAuth 2.0 Protected Resource Metadata document
 * (RFC 9728 / MCP Authorization spec) so that remote MCP clients
 * such as Claude's Custom Connectors can discover which
 * authorization server (Keycloak) protects this resource and
 * drive the user through the correct OAuth login flow.
 */
@RestController
public class ProtectedResourceMetadataController {

    private final String issuerUri;
    private final String resourceUri;

    public ProtectedResourceMetadataController(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${aquagrow.mcp.resource-uri}") String resourceUri) {
        this.issuerUri = issuerUri;
        this.resourceUri = resourceUri;
    }

    @GetMapping("/.well-known/oauth-protected-resource")
    public Map<String, Object> metadata() {
        return Map.of(
                "resource", resourceUri,
                "authorization_servers", List.of(issuerUri),
                "bearer_methods_supported", List.of("header")
        );
    }
}
