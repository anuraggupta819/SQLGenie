package com.anuraggupta.sqlgenie.controller;

import com.anuraggupta.sqlgenie.config.AbstractIntegrationTest;
import com.anuraggupta.sqlgenie.service.ai.GeneratedSql;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Proves the wiring - HTTP, security, service orchestration, real Postgres -
 * works end to end. Business-logic branches (rejected/failed outcomes,
 * ownership checks) are already covered at the unit level
 * (AssistantServiceImplTest, QueryHistoryServiceImplTest,
 * FavoriteQueryServiceImplTest); this only needs the happy paths plus one
 * cross-cutting check (a second user can't see the first user's data).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QueryAndFavoriteControllerIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicBoolean READONLY_ROLE_READY = new AtomicBoolean(false);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;

    @BeforeEach
    void setUp() throws SQLException {
        if (READONLY_ROLE_READY.compareAndSet(false, true)) {
            try (Connection conn = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE ROLE readonly_query_user LOGIN PASSWORD 'readonly_query_password'");
                stmt.execute("GRANT USAGE ON SCHEMA target TO readonly_query_user");
                stmt.execute("GRANT SELECT ON ALL TABLES IN SCHEMA target TO readonly_query_user");
            }
        }

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.system(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Passw0rd!", "fullName", "Test User"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }

    @Test
    void submitQuery_successfulFlow_executesAndRecordsHistory() throws Exception {
        String token = registerAndGetToken("query-flow@example.com");
        when(callResponseSpec.entity(GeneratedSql.class)).thenReturn(
                new GeneratedSql("SELECT id, first_name FROM target.customers ORDER BY id",
                        "Returns every customer's id and first name."));

        String responseBody = mockMvc.perform(post("/api/v1/queries")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("question", "list customers"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.rows").isArray())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        org.assertj.core.api.Assertions.assertThat(json.get("rows").size()).isGreaterThan(0);

        mockMvc.perform(get("/api/v1/queries/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].naturalLanguageQuery").value("list customers"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    void queryHistory_canBeDeletedByOwner() throws Exception {
        String token = registerAndGetToken("history-delete@example.com");
        when(callResponseSpec.entity(GeneratedSql.class)).thenReturn(
                new GeneratedSql("SELECT id FROM target.customers", "Returns customer ids."));

        mockMvc.perform(post("/api/v1/queries")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("question", "to be deleted"))))
                .andExpect(status().isOk());

        String historyBody = mockMvc.perform(get("/api/v1/queries/history")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String historyId = objectMapper.readTree(historyBody).get("content").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/queries/history/" + historyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/queries/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(delete("/api/v1/queries/history/" + historyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void queryHistory_isIsolatedPerUser() throws Exception {
        String tokenA = registerAndGetToken("history-user-a@example.com");
        String tokenB = registerAndGetToken("history-user-b@example.com");
        when(callResponseSpec.entity(GeneratedSql.class)).thenReturn(
                new GeneratedSql("SELECT id FROM target.customers", "Returns customer ids."));

        mockMvc.perform(post("/api/v1/queries")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("question", "user a's question"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/queries/history")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void favorites_saveListAndDelete() throws Exception {
        String token = registerAndGetToken("favorites-flow@example.com");
        when(callResponseSpec.content()).thenReturn("Returns every customer.");

        String createBody = mockMvc.perform(post("/api/v1/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "All customers",
                                "naturalLanguageQuery", "list customers",
                                "generatedSql", "SELECT * FROM target.customers"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.explanation").value("Returns every customer."))
                .andReturn().getResponse().getContentAsString();
        String favoriteId = objectMapper.readTree(createBody).get("id").asText();

        mockMvc.perform(post("/api/v1/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "All customers",
                                "naturalLanguageQuery", "list customers again",
                                "generatedSql", "SELECT * FROM target.customers"))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/favorites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("All customers"));

        mockMvc.perform(delete("/api/v1/favorites/" + favoriteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/favorites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void queries_requireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/queries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("question", "anything"))))
                .andExpect(status().isUnauthorized());
    }
}
