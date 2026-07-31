package com.anuraggupta.sqlgenie.service.sql;

import com.anuraggupta.sqlgenie.exception.QueryExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SqlExecutorServiceImpl implements SqlExecutorService {

    private final SqlValidatorService sqlValidatorService;
    private final JdbcTemplate readOnlyJdbcTemplate;

    public SqlExecutorServiceImpl(
            SqlValidatorService sqlValidatorService,
            @Qualifier("readOnlyJdbcTemplate") JdbcTemplate readOnlyJdbcTemplate) {
        this.sqlValidatorService = sqlValidatorService;
        this.readOnlyJdbcTemplate = readOnlyJdbcTemplate;
    }

    @Override
    @Transactional(transactionManager = "readOnlyTransactionManager", readOnly = true)
    public QueryResult execute(String sql) {
        // Never trust that the caller already validated - re-check
        // independently every time, same principle as SqlValidatorService
        // itself documents.
        sqlValidatorService.validate(sql);

        long start = System.currentTimeMillis();
        RawResult raw;
        try {
            raw = readOnlyJdbcTemplate.query(sql, this::extractResult);
        } catch (DataAccessException e) {
            log.warn("Query execution failed after {}ms: {}", System.currentTimeMillis() - start, sql, e);
            throw new QueryExecutionException("Failed to execute the generated query: " + rootCauseMessage(e));
        }

        long elapsedMs = System.currentTimeMillis() - start;
        return new QueryResult(raw.columns(), raw.rows(), elapsedMs);
    }

    private RawResult extractResult(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            rows.add(row);
        }

        return new RawResult(columns, rows);
    }

    private String rootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private record RawResult(List<String> columns, List<Map<String, Object>> rows) {
    }
}
