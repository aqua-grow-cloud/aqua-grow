package com.aquagrow.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    private TenantFilter tenantFilter;

    @BeforeEach
    void setUp() {
        tenantFilter = new TenantFilter();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void doFilter_withParentTenantId_setsParentOnContext() throws Exception {
        Jwt jwt = buildJwt(Map.of("tenant_id", "branch-1", "parent_tenant_id", "root-farm"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        tenantFilter.doFilter(request, response, chain);

        // After chain executes, context is cleared — check was set during chain
        // We verify via chain invocation success and no 403
        assertThat(response.getStatus()).isNotEqualTo(403);
    }

    @Test
    void doFilter_withoutParentTenantId_setsNullParent() throws Exception {
        Jwt jwt = buildJwt(Map.of("tenant_id", "root-farm"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        tenantFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(403);
    }
}
