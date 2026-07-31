package com.anuraggupta.sqlgenie.service;

import com.anuraggupta.sqlgenie.entity.QueryHistory;
import com.anuraggupta.sqlgenie.entity.QueryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QueryHistoryService {

    QueryHistory record(UUID userId, String naturalLanguageQuery, String generatedSql,
                         String explanation, QueryStatus status, String errorMessage,
                         Integer executionTimeMs, Integer rowCount);

    Page<QueryHistory> getHistory(UUID userId, Pageable pageable);

    void deleteHistory(UUID userId, UUID historyId);
}
