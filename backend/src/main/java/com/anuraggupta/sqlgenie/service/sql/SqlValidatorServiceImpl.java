package com.anuraggupta.sqlgenie.service.sql;

import com.anuraggupta.sqlgenie.exception.UnsafeSqlException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class SqlValidatorServiceImpl implements SqlValidatorService {

    private static final Set<String> ALLOWED_TABLES = Set.of(
            "target.customers", "target.products", "target.orders", "target.order_items");

    @Override
    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new UnsafeSqlException("No SQL was generated for this question");
        }

        String trimmed = sql.trim();

        // A single legitimate SELECT never needs a semicolon. Rejecting any
        // semicolon outright is a decisive, parser-independent guard against
        // statement-chaining attacks - CCJSqlParserUtil.parse() is not
        // guaranteed to reject trailing content after one (confirmed: it
        // does not, for "SELECT ...; DROP TABLE ...").
        if (trimmed.contains(";")) {
            throw new UnsafeSqlException("Only a single statement without a semicolon is allowed");
        }

        Set<String> referencedTables;
        try {
            Statement statement = CCJSqlParserUtil.parse(trimmed);

            if (!(statement instanceof Select select)) {
                throw new UnsafeSqlException("Only SELECT statements are allowed");
            }
            if (!(select instanceof PlainSelect plainSelect)) {
                throw new UnsafeSqlException("Only simple SELECT statements are supported (no UNION)");
            }
            // Postgres allows data-modifying CTEs, e.g.
            // "WITH t AS (DELETE FROM x RETURNING *) SELECT * FROM t" - the
            // outer statement is still a SELECT, so rejecting WITH entirely
            // is what actually closes this, not the root-statement check.
            if (plainSelect.getWithItemsList() != null && !plainSelect.getWithItemsList().isEmpty()) {
                throw new UnsafeSqlException("WITH / CTE queries are not allowed");
            }
            if (plainSelect.getIntoTables() != null && !plainSelect.getIntoTables().isEmpty()) {
                throw new UnsafeSqlException("SELECT INTO is not allowed");
            }

            referencedTables = new TablesNamesFinder<Void>().getTables((Statement) select);
        } catch (UnsafeSqlException e) {
            throw e;
        } catch (Exception e) {
            // Fail closed: JSqlParser does not always fail cleanly (e.g. it
            // throws an internal ClassCastException, not JSQLParserException,
            // for some malformed writable-CTE constructs) - any unexpected
            // failure here must still be a rejection, not an uncaught 500.
            log.warn("Rejecting SQL after unexpected validation error: {}", sql, e);
            throw new UnsafeSqlException("Generated SQL could not be safely validated");
        }

        if (referencedTables.isEmpty()) {
            throw new UnsafeSqlException("Query must reference at least one table");
        }

        for (String table : referencedTables) {
            String normalized = table.toLowerCase(Locale.ROOT).replace("\"", "");
            if (!ALLOWED_TABLES.contains(normalized)) {
                log.warn("Rejected SQL referencing disallowed table '{}': {}", table, sql);
                throw new UnsafeSqlException(
                        "Query references a table that is not allowed: " + table);
            }
        }
    }
}
