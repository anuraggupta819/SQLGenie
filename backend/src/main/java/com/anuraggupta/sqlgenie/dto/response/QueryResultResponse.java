package com.anuraggupta.sqlgenie.dto.response;

import com.anuraggupta.sqlgenie.entity.QueryStatus;
import com.anuraggupta.sqlgenie.service.sql.QueryResult;

import java.util.List;
import java.util.Map;

public record QueryResultResponse(
        QueryStatus status,
        String sql,
        String explanation,
        List<String> columns,
        List<Map<String, Object>> rows,
        Long executionTimeMs,
        String errorMessage
) {

    public static QueryResultResponse success(String sql, String explanation, QueryResult result) {
        return new QueryResultResponse(
                QueryStatus.SUCCESS, sql, explanation, result.columns(), result.rows(),
                result.executionTimeMs(), null);
    }

    public static QueryResultResponse rejected(String reason) {
        return new QueryResultResponse(QueryStatus.REJECTED, null, null, null, null, null, reason);
    }

    public static QueryResultResponse failed(String sql, String reason) {
        return new QueryResultResponse(QueryStatus.FAILED, sql, null, null, null, null, reason);
    }
}
