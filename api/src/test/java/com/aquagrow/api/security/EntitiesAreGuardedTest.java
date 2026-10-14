package com.aquagrow.api.security;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A new entity must not be able to ship without the tenant guard: the check is for every entity or for none. */
class EntitiesAreGuardedTest {

    @Test
    void everyEntityHasAnOwnerAndTheTenantGuard() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<String> problems = new ArrayList<>();
        int count = 0;
        for (var bean : scanner.findCandidateComponents("com.aquagrow.api.model")) {
            count++;
            Class<?> type = Class.forName(bean.getBeanClassName());
            if (!TenantOwned.class.isAssignableFrom(type)) {
                problems.add(type.getSimpleName() + " does not implement TenantOwned");
            }
            EntityListeners listeners = type.getAnnotation(EntityListeners.class);
            if (listeners == null || !Arrays.asList(listeners.value()).contains(TenantGuardListener.class)) {
                problems.add(type.getSimpleName() + " is not guarded by TenantGuardListener");
            }
        }
        assertThat(count).as("entities found").isGreaterThanOrEqualTo(15);
        assertThat(problems).isEmpty();
    }
}
