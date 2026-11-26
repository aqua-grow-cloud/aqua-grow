package com.aquagrow.mcp.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMetadataEntryPointTest {

    @Test
    void missingToken_is401WithBearerChallengePointingAtTheMetadata() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ResourceMetadataEntryPoint("https://mcp.aqua-grow.cloud")
                .commence(new MockHttpServletRequest(), response, new BadCredentialsException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate"))
                .isEqualTo("Bearer resource_metadata=\"https://mcp.aqua-grow.cloud/.well-known/oauth-protected-resource\"");
    }

    @Test
    void invalidToken_keepsTheErrorAndAddsTheMetadata() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ResourceMetadataEntryPoint("https://mcp.aqua-grow.cloud/")
                .commence(new MockHttpServletRequest(), response,
                        new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "expired", null)));

        assertThat(response.getStatus()).isEqualTo(401);
        String header = response.getHeader("WWW-Authenticate");
        assertThat(header).startsWith("Bearer ").contains("error=\"invalid_token\"")
                .endsWith("resource_metadata=\"https://mcp.aqua-grow.cloud/.well-known/oauth-protected-resource\"");
    }
}
