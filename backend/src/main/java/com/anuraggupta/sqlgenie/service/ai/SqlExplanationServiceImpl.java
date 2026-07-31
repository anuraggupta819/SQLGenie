package com.anuraggupta.sqlgenie.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqlExplanationServiceImpl implements SqlExplanationService {

    private static final String PROMPT_TEMPLATE = """
            Explain what the following SQL query does, in plain English, for someone
            who does not know SQL. Describe the result it produces, not SQL syntax.
            Keep it to 1-3 sentences.

            SQL:
            %s
            """;

    private final ChatClient chatClient;

    @Override
    public String explain(String sql) {
        String explanation = chatClient.prompt()
                .user(PROMPT_TEMPLATE.formatted(sql))
                .call()
                .content();

        log.info("Explained SQL ({} chars) -> explanation ({} chars)",
                sql.length(), explanation != null ? explanation.length() : 0);

        return explanation;
    }
}
