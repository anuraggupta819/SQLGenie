package com.anuraggupta.sqlgenie.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NlToSqlServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;
    @Mock
    private SchemaIntrospectionService schemaIntrospectionService;

    private NlToSqlServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NlToSqlServiceImpl(chatClient, schemaIntrospectionService);
    }

    @Test
    void generateSql_buildsPromptWithSchemaAndReturnsStructuredResult() {
        when(schemaIntrospectionService.describeTargetSchema())
                .thenReturn("target.customers(id integer, first_name character varying)");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        GeneratedSql expected = new GeneratedSql(
                "SELECT * FROM target.customers", "Returns all customers");
        when(callResponseSpec.entity(GeneratedSql.class)).thenReturn(expected);

        GeneratedSql result = service.generateSql("show me all customers");

        assertThat(result).isEqualTo(expected);
        verify(requestSpec).system(contains("target.customers(id integer, first_name character varying)"));
        verify(requestSpec).system(contains("Only ever produce a single SELECT statement"));
        verify(requestSpec).user(eq("show me all customers"));
    }

    @Test
    void generateSql_returnsEmptySqlWithExplanation_whenModelCannotAnswer() {
        when(schemaIntrospectionService.describeTargetSchema()).thenReturn("target.customers(id integer)");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        GeneratedSql expected = new GeneratedSql("", "The schema has no data about employee salaries.");
        when(callResponseSpec.entity(GeneratedSql.class)).thenReturn(expected);

        GeneratedSql result = service.generateSql("what is the average employee salary?");

        assertThat(result.sql()).isEmpty();
        assertThat(result.explanation()).contains("employee salaries");
    }
}
