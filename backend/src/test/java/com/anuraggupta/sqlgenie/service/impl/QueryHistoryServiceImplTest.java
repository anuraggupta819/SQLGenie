package com.anuraggupta.sqlgenie.service.impl;

import com.anuraggupta.sqlgenie.entity.User;
import com.anuraggupta.sqlgenie.exception.ResourceNotFoundException;
import com.anuraggupta.sqlgenie.repository.QueryHistoryRepository;
import com.anuraggupta.sqlgenie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryHistoryServiceImplTest {

    @Mock
    private QueryHistoryRepository queryHistoryRepository;
    @Mock
    private UserRepository userRepository;

    private QueryHistoryServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID historyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QueryHistoryServiceImpl(queryHistoryRepository, userRepository);
    }

    @Test
    void deleteHistory_deletes_whenOwnedByUser() {
        when(queryHistoryRepository.findByIdAndUserId(historyId, userId))
                .thenReturn(Optional.of(com.anuraggupta.sqlgenie.entity.QueryHistory.builder().build()));

        assertThatCode(() -> service.deleteHistory(userId, historyId)).doesNotThrowAnyException();

        verify(queryHistoryRepository).deleteByIdAndUserId(historyId, userId);
    }

    @Test
    void deleteHistory_throwsNotFound_whenNotOwnedOrMissing() {
        when(queryHistoryRepository.findByIdAndUserId(historyId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteHistory(userId, historyId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(queryHistoryRepository, never()).deleteByIdAndUserId(any(), any());
    }

    @Test
    void record_savesHistoryUsingUserReference_withoutFetchingUser() {
        User userRef = User.builder().id(userId).build();
        when(userRepository.getReferenceById(userId)).thenReturn(userRef);

        service.record(userId, "question", "SELECT 1", "explanation",
                com.anuraggupta.sqlgenie.entity.QueryStatus.SUCCESS, null, 10, 1);

        verify(userRepository).getReferenceById(userId);
        verify(userRepository, never()).findById(any());
        verify(queryHistoryRepository).save(any());
    }
}
