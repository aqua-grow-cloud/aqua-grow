package com.aquagrow.mcp.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void doFilter_withTenantId_allowsRequestThrough() throws Exception {
        Jwt jwt = buildJwt(Map.of("tenant_id", "root-farm"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        tenantFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(403);
    }

    @Test
    void doFilter_withoutTenantIdClaim_returns403AndDoesNotContinueChain() throws Exception {
        Jwt jwt = buildJwt(Map.of("sub", "user-without-tenant"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        tenantFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void doFilter_withBlankTenantIdClaim_returns403AndDoesNotContinueChain() throws Exception {
        for (String blank : new String[]{"", "   "}) {
            Jwt jwt = buildJwt(Map.of("sub", "user", "tenant_id", blank));
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            tenantFilter.doFilter(new MockHttpServletRequest(), response, chain);

            assertThat(response.getStatus()).as("tenant_id='%s'", blank).isEqualTo(403);
            assertThat(chain.getRequest()).as("chain must not continue").isNull();
        }
    }

    @Test
    void doFilter_alwaysClearsTenantContextAfterChain() throws Exception {
        Jwt jwt = buildJwt(Map.of("tenant_id", "root-farm"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        tenantFilter.doFilter(request, response, chain);

        assertThat(TenantContext.getTenantId()).isNull();
    }
}
