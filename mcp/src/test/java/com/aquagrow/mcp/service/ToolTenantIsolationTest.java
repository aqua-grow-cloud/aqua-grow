package com.aquagrow.mcp.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the tenant-isolation rule: tenant identity comes only from the
 * validated JWT (TenantContext), never from an LLM-supplied tool parameter.
 */
class ToolTenantIsolationTest {

    private static List<Class<?>> mcpServiceClasses() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        List<Class<?>> classes = new ArrayList<>();
        for (var bd : scanner.findCandidateComponents("com.aquagrow.mcp")) {
            classes.add(Class.forName(bd.getBeanClassName()));
        }
        return classes;
    }

    private static List<Method> toolMethods() throws ClassNotFoundException {
        List<Method> methods = new ArrayList<>();
        for (Class<?> c : mcpServiceClasses()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Tool.class)) {
                    methods.add(m);
                }
            }
        }
        return methods;
    }

    @Test
    void toolsAreDiscovered() throws Exception {
        assertThat(toolMethods()).isNotEmpty();
    }

    @Test
    void noToolAcceptsATenantParameter() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (Method m : toolMethods()) {
            for (Parameter p : m.getParameters()) {
                // Relies on -parameters, which the Spring Boot Gradle plugin enables.
                assertThat(p.isNamePresent())
                        .as("parameter names must be compiled in (%s)", m)
                        .isTrue();
                if (p.getName().toLowerCase(Locale.ROOT).contains("tenant")) {
                    offenders.add(m.getDeclaringClass().getSimpleName() + "." + m.getName()
                            + "(" + p.getName() + ")");
                }
            }
        }
        assertThat(offenders).as("tools must not take a tenant identifier").isEmpty();
    }

    @Test
    void noToolDescriptionAsksForATenantId() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (Method m : toolMethods()) {
            String description = m.getAnnotation(Tool.class).description().toLowerCase(Locale.ROOT);
            if (description.contains("tenantid") || description.contains("tenant id")
                    || description.contains("provide tenant")) {
                offenders.add(m.getDeclaringClass().getSimpleName() + "." + m.getName());
            }
        }
        assertThat(offenders).as("tool descriptions must not ask the model for a tenant id").isEmpty();
    }
}
