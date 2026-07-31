package com.anuraggupta.sqlgenie.controller;

import com.anuraggupta.sqlgenie.config.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullAuthLifecycle_registerLoginRefreshLogout() throws Exception {
        Map<String, String> registerBody = Map.of(
                "email", "lifecycle@example.com",
                "password", "Passw0rd!",
                "fullName", "Lifecycle Test"
        );

        String registerJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String firstRefreshToken = objectMapper.readTree(registerJson).get("refreshToken").asText();

        // Duplicate registration must be rejected.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isConflict());

        // Login with correct credentials succeeds.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "lifecycle@example.com",
                                "password", "Passw0rd!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Login with wrong password is rejected.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "lifecycle@example.com",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized());

        // Refresh rotates the token.
        String refreshJson = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(refreshJson).get("refreshToken").asText();

        // Reusing the now-revoked original refresh token is rejected.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized());

        // Logout revokes the current refresh token.
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
                .andExpect(status().isNoContent());

        // Refreshing after logout is rejected.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_rejectsWeakPassword() throws Exception {
        Map<String, String> body = Map.of(
                "email", "weak@example.com",
                "password", "short",
                "fullName", "Weak Password"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void protectedEndpoint_rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"nonexistent\"}"))
                .andExpect(status().isNoContent()); // logout endpoint itself is permitAll and is a no-op for unknown tokens

        mockMvc.perform(post("/api/v1/nonexistent-protected-area"))
                .andExpect(status().isUnauthorized());
    }
}
