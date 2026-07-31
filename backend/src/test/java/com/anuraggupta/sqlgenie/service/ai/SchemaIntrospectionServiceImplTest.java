package com.anuraggupta.sqlgenie.service.ai;

import com.anuraggupta.sqlgenie.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SchemaIntrospectionServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void describeTargetSchema_listsAllSeededTablesAndColumns() {
        SchemaIntrospectionServiceImpl service = new SchemaIntrospectionServiceImpl(jdbcTemplate);

        String description = service.describeTargetSchema();

        assertThat(description).contains("target.customers(");
        assertThat(description).contains("target.products(");
        assertThat(description).contains("target.orders(");
        assertThat(description).contains("target.order_items(");
        assertThat(description).contains("id integer");
        assertThat(description).contains("email character varying");
        // The app schema must never leak into the prompt context.
        assertThat(description).doesNotContain("app.users").doesNotContain("password_hash");
    }
}
