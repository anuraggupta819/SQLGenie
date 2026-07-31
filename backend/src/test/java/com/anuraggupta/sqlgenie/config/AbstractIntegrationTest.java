package com.anuraggupta.sqlgenie.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for tests that need a real PostgreSQL instance.
 * Requires a working Docker daemon - runs in CI, may not run on every dev machine.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    // withInitScript runs once, right after the container starts and before
    // Spring/Flyway ever connects - creates readonly_query_user so
    // V5__grant_readonly_role_access.sql (which every test's Flyway run
    // executes) doesn't fail with "role does not exist" in tests that never
    // otherwise touch that role.
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("testcontainers-init.sql");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
