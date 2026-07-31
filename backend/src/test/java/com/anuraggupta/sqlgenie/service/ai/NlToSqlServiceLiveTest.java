package com.anuraggupta.sqlgenie.service.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real, non-mocked call against whichever OpenAI-compatible provider is
 * configured (currently Groq). Only runs when GROQ_API_KEY is set, so it's
 * silently skipped in CI and on any machine without a key - this is a local,
 * manual verification tool, not part of the regular test gate. Requires the
 * "dev" profile's local Postgres (with the target schema seeded) to be
 * running, since NlToSqlService needs real schema introspection.
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
class NlToSqlServiceLiveTest {

    @Autowired
    private NlToSqlService nlToSqlService;

    @Test
    void generateSql_liveCall_producesValidSelectOnAllowedTable() {
        GeneratedSql result = nlToSqlService.generateSql(
                "What are the first and last names of all customers from India?");

        assertThat(result.sql()).isNotBlank();
        assertThat(result.sql().toLowerCase()).contains("target.customers");
        assertThat(result.explanation()).isNotBlank();
    }
}
