package com.anuraggupta.sqlgenie.service.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real, non-mocked call - see NlToSqlServiceLiveTest for why this is gated
 * and skipped by default rather than part of the regular test gate.
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
class SqlExplanationServiceLiveTest {

    @Autowired
    private SqlExplanationService sqlExplanationService;

    @Test
    void explain_liveCall_describesResultNotSyntax() {
        String explanation = sqlExplanationService.explain(
                "SELECT first_name, last_name FROM target.customers WHERE country = 'India'");

        assertThat(explanation).isNotBlank();
        // Loosely checks it's talking about customers/India, not reciting SQL keywords.
        assertThat(explanation.toLowerCase()).containsAnyOf("customer", "india");
    }
}
