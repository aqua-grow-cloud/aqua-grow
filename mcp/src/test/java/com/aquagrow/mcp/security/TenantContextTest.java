package com.aquagrow.mcp.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void getTenantId_afterSet_returnsValue() {
        TenantContext.setTenantId("root-farm");
        assertThat(TenantContext.getTenantId()).isEqualTo("root-farm");
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
    void clear_removesTenantAndParent() {
        TenantContext.setTenantId("branch-1");
        TenantContext.setParentTenantId("root-farm");
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(TenantContext.getParentTenantId()).isNull();
    }
}
