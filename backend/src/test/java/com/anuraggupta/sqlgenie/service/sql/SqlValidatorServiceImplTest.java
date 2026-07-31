package com.anuraggupta.sqlgenie.service.sql;

import com.anuraggupta.sqlgenie.exception.UnsafeSqlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlValidatorServiceImplTest {

    private final SqlValidatorService validator = new SqlValidatorServiceImpl();

    @Test
    void allows_simpleSelectOnAllowedTable() {
        assertThatCode(() -> validator.validate("SELECT id, first_name FROM target.customers"))
                .doesNotThrowAnyException();
    }

    @Test
    void allows_joinAcrossAllowedTables() {
        assertThatCode(() -> validator.validate("""
                SELECT c.first_name, o.total_amount
                FROM target.customers c
                JOIN target.orders o ON o.customer_id = c.id
                WHERE o.status = 'DELIVERED'
                """)).doesNotThrowAnyException();
    }

    @Test
    void allows_caseInsensitiveTableName() {
        assertThatCode(() -> validator.validate("SELECT * FROM Target.Customers"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO target.customers (first_name) VALUES ('x')",
            "UPDATE target.customers SET first_name = 'x'",
            "DELETE FROM target.customers",
            "DROP TABLE target.customers",
            "TRUNCATE TABLE target.customers"
    })
    void rejects_nonSelectStatements(String sql) {
        assertThatThrownBy(() -> validator.validate(sql))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_selectInto() {
        assertThatThrownBy(() -> validator.validate(
                "SELECT * INTO new_table FROM target.customers"))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_tableOutsideAllowList() {
        assertThatThrownBy(() -> validator.validate("SELECT * FROM app.users"))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_unqualifiedTableName() {
        assertThatThrownBy(() -> validator.validate("SELECT * FROM customers"))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_queryWithNoTableReference() {
        assertThatThrownBy(() -> validator.validate("SELECT pg_sleep(100)"))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_multipleStatements() {
        assertThatThrownBy(() -> validator.validate(
                "SELECT * FROM target.customers; DROP TABLE target.customers"))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_unionSelect() {
        assertThatThrownBy(() -> validator.validate(
                "SELECT id FROM target.customers UNION SELECT id FROM target.products"))
                .isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_blankOrNullSql() {
        assertThatThrownBy(() -> validator.validate("")).isInstanceOf(UnsafeSqlException.class);
        assertThatThrownBy(() -> validator.validate("   ")).isInstanceOf(UnsafeSqlException.class);
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(UnsafeSqlException.class);
    }

    @Test
    void rejects_writableCteDisguisedAsSelect() {
        // WITH t AS (DELETE FROM target.customers RETURNING *) SELECT * FROM t
        // Documents actual behavior against this well-known bypass pattern -
        // whether JSqlParser rejects it as unparseable or our PlainSelect/table
        // checks catch it, the end result must be a rejection either way.
        assertThatThrownBy(() -> validator.validate(
                "WITH t AS (DELETE FROM target.customers RETURNING *) SELECT * FROM t"))
                .isInstanceOf(UnsafeSqlException.class);
    }
}
