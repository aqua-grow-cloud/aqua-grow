package com.aquagrow.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantContext Tests")
class TenantContextTest {

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should set and get tenant ID")
    void setTenantId_shouldStoreTenantId() {
        // When
        TenantContext.setTenantId("test-tenant-123");

        // Then
        assertThat(TenantContext.getTenantId()).isEqualTo("test-tenant-123");
    }

    @Test
    @DisplayName("Should return null when no tenant ID is set")
    void getTenantId_shouldReturnNull_whenNotSet() {
        // When
        String tenantId = TenantContext.getTenantId();

        // Then
        assertThat(tenantId).isNull();
    }

    @Test
    @DisplayName("Should clear tenant ID")
    void clear_shouldRemoveTenantId() {
        // Given
        TenantContext.setTenantId("test-tenant-123");

        // When
        TenantContext.clear();

        // Then
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Should handle multiple tenant IDs in different threads")
    void multipleThreads_shouldIsolateTenantIds() throws InterruptedException {
        // Given
        Thread thread1 = new Thread(() -> {
            TenantContext.setTenantId("tenant-1");
            try {
                Thread.sleep(100);
                assertThat(TenantContext.getTenantId()).isEqualTo("tenant-1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                TenantContext.clear();
            }
        });

        Thread thread2 = new Thread(() -> {
            TenantContext.setTenantId("tenant-2");
            try {
                Thread.sleep(100);
                assertThat(TenantContext.getTenantId()).isEqualTo("tenant-2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                TenantContext.clear();
            }
        });

        // When
        thread1.start();
        thread2.start();

        // Then
        thread1.join();
        thread2.join();
    }

    @Test
    @DisplayName("Should override previous tenant ID")
    void setTenantId_shouldOverridePreviousValue() {
        // Given
        TenantContext.setTenantId("tenant-1");

        // When
        TenantContext.setTenantId("tenant-2");

        // Then
        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-2");
    }
}
