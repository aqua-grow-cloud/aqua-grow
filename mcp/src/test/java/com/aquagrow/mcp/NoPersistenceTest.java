package com.aquagrow.mcp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The MCP server reads and writes everything through aqua-grow-api and never touches the database.
 * These tests fail if a database dependency or a persistence annotation creeps back in, which would
 * bring back the schema coupling, the duplicated tenant rules and the lazy-loading serialization bugs
 * this design removed.
 */
class NoPersistenceTest {

    private static final List<String> FORBIDDEN_IN_SOURCE = List.of(
            "jakarta.persistence", "javax.persistence", "org.hibernate", "org.springframework.data",
            "org.springframework.transaction", "org.postgresql", "org.aspectj", "javax.sql", "java.sql",
            "@Entity", "@Transactional", "Repository<");

    private static List<Path> javaFiles(String root) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void noMainSourceFileMentionsPersistence() throws IOException {
        List<Path> files = javaFiles("src/main/java");
        assertThat(files).isNotEmpty();

        for (Path file : files) {
            String content = Files.readString(file);
            for (String forbidden : FORBIDDEN_IN_SOURCE) {
                assertThat(content).as("%s must not contain '%s'", file, forbidden).doesNotContain(forbidden);
            }
        }
    }

    @Test
    void noRepositoryOrEntityPackagesRemain() {
        assertThat(Path.of("src/main/java/com/aquagrow/mcp/repository")).doesNotExist();
        assertThat(Path.of("src/main/java/com/aquagrow/mcp/config/TenantFilterActivator.java")).doesNotExist();
        assertThat(Path.of("src/main/java/com/aquagrow/mcp/model/package-info.java")).doesNotExist();
    }

    @Test
    void theDatabaseLibrariesAreNotOnTheClasspath() {
        for (String type : List.of("org.hibernate.Session", "org.postgresql.Driver",
                "org.springframework.data.jpa.repository.JpaRepository", "org.aspectj.lang.JoinPoint")) {
            assertThatThrownBy(() -> Class.forName(type))
                    .as("%s should not be available", type)
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void noConfigurationMentionsADatabase() throws IOException {
        String yml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(yml).doesNotContain("datasource").doesNotContain("jpa:").doesNotContain("hibernate")
                .doesNotContain("liquibase").doesNotContain("postgres");
    }
}
