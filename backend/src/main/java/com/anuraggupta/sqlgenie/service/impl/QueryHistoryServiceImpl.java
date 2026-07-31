package com.anuraggupta.sqlgenie.service.impl;

import com.anuraggupta.sqlgenie.entity.QueryHistory;
import com.anuraggupta.sqlgenie.entity.QueryStatus;
import com.anuraggupta.sqlgenie.entity.User;
import com.anuraggupta.sqlgenie.exception.ResourceNotFoundException;
import com.anuraggupta.sqlgenie.repository.QueryHistoryRepository;
import com.anuraggupta.sqlgenie.repository.UserRepository;
import com.anuraggupta.sqlgenie.service.QueryHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryHistoryServiceImpl implements QueryHistoryService {

    private final QueryHistoryRepository queryHistoryRepository;
    private final UserRepository userRepository;

    @Override
    public QueryHistory record(UUID userId, String naturalLanguageQuery, String generatedSql,
                                String explanation, QueryStatus status, String errorMessage,
                                Integer executionTimeMs, Integer rowCount) {
        User userRef = userRepository.getReferenceById(userId);
        QueryHistory history = QueryHistory.builder()
                .user(userRef)
                .naturalLanguageQuery(naturalLanguageQuery)
                .generatedSql(generatedSql)
                .explanation(explanation)
                .status(status)
                .errorMessage(errorMessage)
                .executionTimeMs(executionTimeMs)
                .rowCount(rowCount)
                .build();
        return queryHistoryRepository.save(history);
    }

    @Override
    public Page<QueryHistory> getHistory(UUID userId, Pageable pageable) {
        return queryHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public void deleteHistory(UUID userId, UUID historyId) {
        queryHistoryRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No history entry found with that id"));
        queryHistoryRepository.deleteByIdAndUserId(historyId, userId);
    }
}
