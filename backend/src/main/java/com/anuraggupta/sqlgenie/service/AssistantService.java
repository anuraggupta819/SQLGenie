package com.anuraggupta.sqlgenie.service;

import com.anuraggupta.sqlgenie.dto.response.QueryResultResponse;

import java.util.UUID;

public interface AssistantService {

    /**
     * Full pipeline: natural language -> SQL -> validate -> execute -> persist
     * history. Never throws for an "expected" bad outcome (unanswerable
     * question, unsafe SQL, execution failure) - those are legitimate
     * results, reflected in the response's status field, not HTTP errors.
     */
    QueryResultResponse handle(UUID userId, String naturalLanguageQuery);
}
