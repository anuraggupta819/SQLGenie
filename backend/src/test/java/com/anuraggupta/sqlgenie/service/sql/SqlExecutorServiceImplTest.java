package com.anuraggupta.sqlgenie.service.sql;

import com.anuraggupta.sqlgenie.config.AbstractIntegrationTest;
import com.anuraggupta.sqlgenie.exception.QueryExecutionException;
import com.anuraggupta.sqlgenie.exception.UnsafeSqlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real, database-enforced readonly_query_user role - not
 * just the test container's default superuser - by creating that role
 * dynamically (mirroring backend/scripts/local-db-setup.sql) before the
 * first test. This is what lets execute_enforcesStatementTimeout below
 * prove the timeout actually cancels a running query, not just that a
 * config value is set.
 */
@SpringBootTest
@ActiveProfiles("test")
class SqlExecutorServiceImplTest extends AbstractIntegrationTest {

    private static final AtomicBoolean READONLY_ROLE_READY = new AtomicBoolean(false);

    @Autowired
    private SqlExecutorService sqlExecutorService;

    @BeforeEach
    void ensureReadOnlyRoleExists() throws SQLException {
        if (READONLY_ROLE_READY.compareAndSet(false, true)) {
            try (Connection conn = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE ROLE readonly_query_user LOGIN PASSWORD 'readonly_query_password'");
                stmt.execute("GRANT USAGE ON SCHEMA target TO readonly_query_user");
                stmt.execute("GRANT SELECT ON ALL TABLES IN SCHEMA target TO readonly_query_user");
            }
        }
    }

    @Test
    void execute_returnsColumnsAndRows_forValidQuery() {
        QueryResult result = sqlExecutorService.execute(
                "SELECT id, first_name FROM target.customers ORDER BY id");

        assertThat(result.columns()).containsExactly("id", "first_name");
        assertThat(result.rows()).hasSize(5); // capped by test's max-rows=5, seeded data has 10
        assertThat(result.rows().get(0)).containsKeys("id", "first_name");
        assertThat(result.executionTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void execute_truncatesAtConfiguredRowLimit() {
        QueryResult result = sqlExecutorService.execute("SELECT * FROM target.customers");

        assertThat(result.rows()).hasSize(5); // application-test.yml sets max-rows: 5
    }

    @Test
    void execute_enforcesStatementTimeout_onSlowQuery() {
        long start = System.currentTimeMillis();

        // References an allowed table (passes the validator) but is slow due
        // to pg_sleep - this is exactly the class of risk the validator
        // deliberately does not try to block (see SqlValidatorServiceImpl),
        // bounded instead by this timeout.
        assertThatThrownBy(() -> sqlExecutorService.execute(
                "SELECT *, pg_sleep(10) FROM target.customers"))
                .isInstanceOf(QueryExecutionException.class);

        long elapsedMs = System.currentTimeMillis() - start;
        // application-test.yml sets timeout-seconds: 2 - must not have waited for the full 10s sleep.
        assertThat(elapsedMs).isLessThan(8000);
    }

    @Test
    void execute_rejectsUnsafeSql_viaInternalValidatorCheck() {
        assertThatThrownBy(() -> sqlExecutorService.execute("DELETE FROM target.customers"))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void execute_wrapsSqlErrors_inQueryExecutionException() {
        assertThatThrownBy(() -> sqlExecutorService.execute(
                "SELECT nonexistent_column FROM target.customers"))
                .isInstanceOf(QueryExecutionException.class);
    }
}
