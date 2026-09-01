package com.aquagrow.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void isRootTenant_whenNoParent_returnsTrue() {
        TenantContext.setTenantId("root-farm");
        assertThat(TenantContext.isRootTenant()).isTrue();
        assertThat(TenantContext.isSubTenant()).isFalse();
    }

    @Test
    void isSubTenant_whenParentSet_returnsTrue() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");
        assertThat(TenantContext.isSubTenant()).isTrue();
        assertThat(TenantContext.isRootTenant()).isFalse();
    }

    @Test
    void getEffectiveFeedTenantId_forSubTenant_returnsParent() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");
        assertThat(TenantContext.getEffectiveFeedTenantId()).isEqualTo("root-farm");
    }

    @Test
    void getEffectiveFeedTenantId_forRootTenant_returnsSelf() {
        TenantContext.setTenantId("root-farm");
        assertThat(TenantContext.getEffectiveFeedTenantId()).isEqualTo("root-farm");
    }

    @Test
    void clear_removesParentTenantId() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");
        TenantContext.clear();
        assertThat(TenantContext.getParentTenantId()).isNull();
        assertThat(TenantContext.isSubTenant()).isFalse();
    }
}
