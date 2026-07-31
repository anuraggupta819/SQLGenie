package com.anuraggupta.sqlgenie.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlExplanationServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SqlExplanationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SqlExplanationServiceImpl(chatClient);
    }

    @Test
    void explain_buildsPromptWithSqlAndReturnsPlainTextExplanation() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Returns every customer located in India.");

        String result = service.explain("SELECT * FROM target.customers WHERE country = 'India'");

        assertThat(result).isEqualTo("Returns every customer located in India.");
        verify(requestSpec).user(contains("SELECT * FROM target.customers WHERE country = 'India'"));
    }
}
