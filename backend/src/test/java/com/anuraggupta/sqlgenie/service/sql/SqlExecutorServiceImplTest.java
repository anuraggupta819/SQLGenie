package com.anuraggupta.sqlgenie.service.sql;

import com.anuraggupta.sqlgenie.config.AbstractIntegrationTest;
import com.anuraggupta.sqlgenie.exception.QueryExecutionException;
import com.anuraggupta.sqlgenie.exception.UnsafeSqlException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real, database-enforced readonly_query_user role - not
 * just the test container's default superuser. The role and its grants
 * (AbstractIntegrationTest's init script + V5__grant_readonly_role_access.sql)
 * are already in place by the time any test runs. This is what lets
 * execute_enforcesStatementTimeout below prove the timeout actually cancels
 * a running query, not just that a config value is set.
 */
@SpringBootTest
@ActiveProfiles("test")
class SqlExecutorServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private SqlExecutorService sqlExecutorService;

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
