package com.anuraggupta.sqlgenie.service.impl;

import com.anuraggupta.sqlgenie.dto.response.QueryResultResponse;
import com.anuraggupta.sqlgenie.entity.QueryStatus;
import com.anuraggupta.sqlgenie.exception.QueryExecutionException;
import com.anuraggupta.sqlgenie.exception.UnsafeSqlException;
import com.anuraggupta.sqlgenie.service.AssistantService;
import com.anuraggupta.sqlgenie.service.QueryHistoryService;
import com.anuraggupta.sqlgenie.service.ai.GeneratedSql;
import com.anuraggupta.sqlgenie.service.ai.NlToSqlService;
import com.anuraggupta.sqlgenie.service.sql.QueryResult;
import com.anuraggupta.sqlgenie.service.sql.SqlExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantServiceImpl implements AssistantService {

    private final NlToSqlService nlToSqlService;
    private final SqlExecutorService sqlExecutorService;
    private final QueryHistoryService queryHistoryService;

    @Override
    public QueryResultResponse handle(UUID userId, String naturalLanguageQuery) {
        GeneratedSql generated = nlToSqlService.generateSql(naturalLanguageQuery);

        if (generated.sql() == null || generated.sql().isBlank()) {
            queryHistoryService.record(userId, naturalLanguageQuery, null, generated.explanation(),
                    QueryStatus.REJECTED, generated.explanation(), null, null);
            return QueryResultResponse.rejected(generated.explanation());
        }

        try {
            // SqlExecutorService independently re-validates internally - this
            // call is what actually enforces safety, not a check made here.
            QueryResult result = sqlExecutorService.execute(generated.sql());

            queryHistoryService.record(userId, naturalLanguageQuery, generated.sql(),
                    generated.explanation(), QueryStatus.SUCCESS, null,
                    (int) result.executionTimeMs(), result.rows().size());

            return QueryResultResponse.success(generated.sql(), generated.explanation(), result);
        } catch (UnsafeSqlException e) {
            queryHistoryService.record(userId, naturalLanguageQuery, generated.sql(),
                    generated.explanation(), QueryStatus.REJECTED, e.getMessage(), null, null);
            return QueryResultResponse.rejected(e.getMessage());
        } catch (QueryExecutionException e) {
            queryHistoryService.record(userId, naturalLanguageQuery, generated.sql(),
                    generated.explanation(), QueryStatus.FAILED, e.getMessage(), null, null);
            return QueryResultResponse.failed(generated.sql(), e.getMessage());
        }
    }
}
