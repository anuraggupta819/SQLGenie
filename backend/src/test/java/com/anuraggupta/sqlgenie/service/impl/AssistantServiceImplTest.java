package com.anuraggupta.sqlgenie.service.impl;

import com.anuraggupta.sqlgenie.dto.response.QueryResultResponse;
import com.anuraggupta.sqlgenie.entity.QueryStatus;
import com.anuraggupta.sqlgenie.exception.QueryExecutionException;
import com.anuraggupta.sqlgenie.exception.UnsafeSqlException;
import com.anuraggupta.sqlgenie.service.QueryHistoryService;
import com.anuraggupta.sqlgenie.service.ai.GeneratedSql;
import com.anuraggupta.sqlgenie.service.ai.NlToSqlService;
import com.anuraggupta.sqlgenie.service.sql.QueryResult;
import com.anuraggupta.sqlgenie.service.sql.SqlExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssistantServiceImplTest {

    @Mock
    private NlToSqlService nlToSqlService;
    @Mock
    private SqlExecutorService sqlExecutorService;
    @Mock
    private QueryHistoryService queryHistoryService;

    private AssistantServiceImpl assistantService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assistantService = new AssistantServiceImpl(nlToSqlService, sqlExecutorService, queryHistoryService);
    }

    @Test
    void handle_returnsSuccessAndRecordsHistory_whenExecutionSucceeds() {
        GeneratedSql generated = new GeneratedSql("SELECT * FROM target.customers", "Returns all customers");
        when(nlToSqlService.generateSql("show customers")).thenReturn(generated);

        QueryResult result = new QueryResult(
                List.of("id", "first_name"), List.of(Map.of("id", 1, "first_name", "Alice")), 42L);
        when(sqlExecutorService.execute(generated.sql())).thenReturn(result);

        QueryResultResponse response = assistantService.handle(userId, "show customers");

        assertThat(response.status()).isEqualTo(QueryStatus.SUCCESS);
        assertThat(response.sql()).isEqualTo(generated.sql());
        assertThat(response.rows()).hasSize(1);
        assertThat(response.errorMessage()).isNull();

        verify(queryHistoryService).record(eq(userId), eq("show customers"), eq(generated.sql()),
                eq(generated.explanation()), eq(QueryStatus.SUCCESS), isNull(), eq(42), eq(1));
    }

    @Test
    void handle_returnsRejected_whenModelCannotAnswer() {
        GeneratedSql generated = new GeneratedSql("", "No data about employee salaries exists.");
        when(nlToSqlService.generateSql("average salary?")).thenReturn(generated);

        QueryResultResponse response = assistantService.handle(userId, "average salary?");

        assertThat(response.status()).isEqualTo(QueryStatus.REJECTED);
        assertThat(response.errorMessage()).isEqualTo("No data about employee salaries exists.");
        verifyNoInteractions(sqlExecutorService);
        verify(queryHistoryService).record(eq(userId), eq("average salary?"), isNull(),
                eq(generated.explanation()), eq(QueryStatus.REJECTED), eq(generated.explanation()),
                isNull(), isNull());
    }

    @Test
    void handle_returnsRejected_whenExecutorRejectsAsUnsafe() {
        GeneratedSql generated = new GeneratedSql("DELETE FROM target.customers", "Deletes customers");
        when(nlToSqlService.generateSql("delete customers")).thenReturn(generated);
        when(sqlExecutorService.execute(generated.sql()))
                .thenThrow(new UnsafeSqlException("Only SELECT statements are allowed"));

        QueryResultResponse response = assistantService.handle(userId, "delete customers");

        assertThat(response.status()).isEqualTo(QueryStatus.REJECTED);
        assertThat(response.errorMessage()).isEqualTo("Only SELECT statements are allowed");
        verify(queryHistoryService).record(eq(userId), anyString(), eq(generated.sql()),
                anyString(), eq(QueryStatus.REJECTED), eq("Only SELECT statements are allowed"),
                isNull(), isNull());
    }

    @Test
    void handle_returnsFailed_whenExecutionThrows() {
        GeneratedSql generated = new GeneratedSql(
                "SELECT nonexistent_column FROM target.customers", "Selects a column");
        when(nlToSqlService.generateSql("bad column")).thenReturn(generated);
        when(sqlExecutorService.execute(generated.sql()))
                .thenThrow(new QueryExecutionException("Failed to execute the generated query: column missing"));

        QueryResultResponse response = assistantService.handle(userId, "bad column");

        assertThat(response.status()).isEqualTo(QueryStatus.FAILED);
        assertThat(response.sql()).isEqualTo(generated.sql());
        assertThat(response.errorMessage()).contains("column missing");
        verify(queryHistoryService).record(eq(userId), anyString(), eq(generated.sql()),
                anyString(), eq(QueryStatus.FAILED), contains("column missing"), isNull(), isNull());
    }
}
