package com.aquagrow.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TenantAuthorizationService {

    /**
     * Throws SubTenantAccessDeniedException (HTTP 403) if the current request
     * is made by a sub-tenant. Only root tenants may call methods guarded by this.
     */
    public void requireRootTenant() {
        if (TenantContext.isSubTenant()) {
            log.warn("Sub-tenant {} attempted a root-only operation",
                    TenantContext.getTenantId());
            throw new SubTenantAccessDeniedException(
                    "Only the root tenant can perform this action");
        }
    }
}
