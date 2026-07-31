package com.anuraggupta.sqlgenie.dto.response;

import com.anuraggupta.sqlgenie.entity.QueryHistory;
import com.anuraggupta.sqlgenie.entity.QueryStatus;

import java.time.Instant;
import java.util.UUID;

public record QueryHistoryResponse(
        UUID id,
        String naturalLanguageQuery,
        String generatedSql,
        String explanation,
        QueryStatus status,
        String errorMessage,
        Integer executionTimeMs,
        Integer rowCount,
        Instant createdAt
) {

    public static QueryHistoryResponse from(QueryHistory entity) {
        return new QueryHistoryResponse(
                entity.getId(),
                entity.getNaturalLanguageQuery(),
                entity.getGeneratedSql(),
                entity.getExplanation(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getExecutionTimeMs(),
                entity.getRowCount(),
                entity.getCreatedAt()
        );
    }
}
