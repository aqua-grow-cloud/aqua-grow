package com.aquagrow.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String tenantId = jwt.getClaimAsString("tenantId");

                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                } else {
                    // Fail securely if no tenant ID is present in the token
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "No Tenant ID found in token");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            // Always clear context to prevent leakage in thread pools
            TenantContext.clear();
        }
    }
}
