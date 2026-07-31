package com.anuraggupta.sqlgenie.service.impl;

import com.anuraggupta.sqlgenie.dto.request.FavoriteRequest;
import com.anuraggupta.sqlgenie.dto.response.FavoriteResponse;
import com.anuraggupta.sqlgenie.entity.FavoriteQuery;
import com.anuraggupta.sqlgenie.entity.User;
import com.anuraggupta.sqlgenie.exception.DuplicateFavoriteNameException;
import com.anuraggupta.sqlgenie.exception.ResourceNotFoundException;
import com.anuraggupta.sqlgenie.repository.FavoriteQueryRepository;
import com.anuraggupta.sqlgenie.repository.UserRepository;
import com.anuraggupta.sqlgenie.service.ai.SqlExplanationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteQueryServiceImplTest {

    @Mock
    private FavoriteQueryRepository favoriteQueryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SqlExplanationService sqlExplanationService;

    private FavoriteQueryServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FavoriteQueryServiceImpl(favoriteQueryRepository, userRepository, sqlExplanationService);
    }

    @Test
    void save_rejectsDuplicateName() {
        FavoriteRequest request = new FavoriteRequest("My favorite", "top customers", "SELECT 1");
        when(favoriteQueryRepository.existsByUserIdAndName(userId, "My favorite")).thenReturn(true);

        assertThatThrownBy(() -> service.save(userId, request))
                .isInstanceOf(DuplicateFavoriteNameException.class);

        verify(favoriteQueryRepository, never()).save(any());
    }

    @Test
    void save_persistsAndReturnsFreshlyComputedExplanation() {
        FavoriteRequest request = new FavoriteRequest("My favorite", "top customers", "SELECT 1");
        when(favoriteQueryRepository.existsByUserIdAndName(userId, "My favorite")).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(sqlExplanationService.explain("SELECT 1")).thenReturn("Returns the literal value 1.");

        FavoriteResponse response = service.save(userId, request);

        assertThat(response.name()).isEqualTo("My favorite");
        assertThat(response.explanation()).isEqualTo("Returns the literal value 1.");

        ArgumentCaptor<FavoriteQuery> captor = ArgumentCaptor.forClass(FavoriteQuery.class);
        verify(favoriteQueryRepository).save(captor.capture());
        assertThat(captor.getValue().getGeneratedSql()).isEqualTo("SELECT 1");
    }

    @Test
    void getFavorites_explainsEachFavoriteIndependently() {
        FavoriteQuery fav1 = FavoriteQuery.builder().id(UUID.randomUUID()).name("A").generatedSql("SELECT 1").build();
        FavoriteQuery fav2 = FavoriteQuery.builder().id(UUID.randomUUID()).name("B").generatedSql("SELECT 2").build();
        when(favoriteQueryRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(fav1, fav2));
        when(sqlExplanationService.explain(anyString())).thenReturn("some explanation");

        List<FavoriteResponse> responses = service.getFavorites(userId);

        assertThat(responses).hasSize(2);
        verify(sqlExplanationService).explain("SELECT 1");
        verify(sqlExplanationService).explain("SELECT 2");
    }

    @Test
    void deleteFavorite_throwsNotFound_whenNotOwnedOrMissing() {
        when(favoriteQueryRepository.existsByIdAndUserId(any(), eq(userId))).thenReturn(false);

        UUID favoriteId = UUID.randomUUID();
        assertThatThrownBy(() -> service.deleteFavorite(userId, favoriteId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoriteQueryRepository, never()).deleteByIdAndUserId(any(), any());
    }
}
