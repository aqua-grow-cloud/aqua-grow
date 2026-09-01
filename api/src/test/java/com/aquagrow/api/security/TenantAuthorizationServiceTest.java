package com.aquagrow.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TenantAuthorizationServiceTest {

    private final TenantAuthorizationService service = new TenantAuthorizationService();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void requireRootTenant_forRootTenant_doesNotThrow() {
        TenantContext.setTenantId("root-farm");
        assertThatNoException().isThrownBy(service::requireRootTenant);
    }

    @Test
    void requireRootTenant_forSubTenant_throwsSubTenantAccessDeniedException() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");
        assertThatThrownBy(service::requireRootTenant)
                .isInstanceOf(SubTenantAccessDeniedException.class)
                .hasMessageContaining("Only the root tenant can perform this action");
    }
}
