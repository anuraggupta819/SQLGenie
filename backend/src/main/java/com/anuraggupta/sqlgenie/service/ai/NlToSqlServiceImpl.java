package com.anuraggupta.sqlgenie.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NlToSqlServiceImpl implements NlToSqlService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a PostgreSQL expert that converts a natural language question into
            a single, read-only SQL query, plus a plain-English explanation of that query.

            Rules:
            - Only ever produce a single SELECT statement. Never produce INSERT, UPDATE,
              DELETE, DROP, ALTER, TRUNCATE, GRANT, or any statement that writes data or
              changes schema.
            - Only reference the tables and columns listed in the schema below, always
              qualified with the "target." schema prefix (e.g. target.customers). Never
              invent a table or column that is not listed.
            - Never include more than one SQL statement, and never use a semicolon to
              chain statements together.
            - If the question cannot be answered using only the schema below, return an
              empty string for sql and explain why in the explanation field.

            Schema:
            %s
            """;

    private final ChatClient chatClient;
    private final SchemaIntrospectionService schemaIntrospectionService;

    @Override
    public GeneratedSql generateSql(String naturalLanguageQuery) {
        String schemaDescription = schemaIntrospectionService.describeTargetSchema();
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(schemaDescription);

        GeneratedSql result = chatClient.prompt()
                .system(systemPrompt)
                .user(naturalLanguageQuery)
                .call()
                .entity(GeneratedSql.class);

        log.info("Generated SQL for question '{}': {}", naturalLanguageQuery,
                result != null ? result.sql() : null);

        return result;
    }
}
