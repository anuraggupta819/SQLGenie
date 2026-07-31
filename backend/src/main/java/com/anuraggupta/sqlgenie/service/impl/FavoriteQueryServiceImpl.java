package com.anuraggupta.sqlgenie.service.impl;

import com.anuraggupta.sqlgenie.dto.request.FavoriteRequest;
import com.anuraggupta.sqlgenie.dto.response.FavoriteResponse;
import com.anuraggupta.sqlgenie.entity.FavoriteQuery;
import com.anuraggupta.sqlgenie.entity.User;
import com.anuraggupta.sqlgenie.exception.DuplicateFavoriteNameException;
import com.anuraggupta.sqlgenie.exception.ResourceNotFoundException;
import com.anuraggupta.sqlgenie.repository.FavoriteQueryRepository;
import com.anuraggupta.sqlgenie.repository.UserRepository;
import com.anuraggupta.sqlgenie.service.FavoriteQueryService;
import com.anuraggupta.sqlgenie.service.ai.SqlExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteQueryServiceImpl implements FavoriteQueryService {

    private final FavoriteQueryRepository favoriteQueryRepository;
    private final UserRepository userRepository;
    private final SqlExplanationService sqlExplanationService;

    @Override
    public FavoriteResponse save(UUID userId, FavoriteRequest request) {
        if (favoriteQueryRepository.existsByUserIdAndName(userId, request.name())) {
            throw new DuplicateFavoriteNameException(
                    "You already have a favorite named '" + request.name() + "'");
        }

        User userRef = userRepository.getReferenceById(userId);
        FavoriteQuery favorite = FavoriteQuery.builder()
                .user(userRef)
                .name(request.name())
                .naturalLanguageQuery(request.naturalLanguageQuery())
                .generatedSql(request.generatedSql())
                .build();
        favoriteQueryRepository.save(favorite);

        String explanation = sqlExplanationService.explain(favorite.getGeneratedSql());
        return FavoriteResponse.from(favorite, explanation);
    }

    @Override
    public List<FavoriteResponse> getFavorites(UUID userId) {
        return favoriteQueryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(favorite -> FavoriteResponse.from(
                        favorite, sqlExplanationService.explain(favorite.getGeneratedSql())))
                .toList();
    }

    @Override
    @Transactional
    public void deleteFavorite(UUID userId, UUID favoriteId) {
        // deleteByIdAndUserId is a custom derived query method, not one
        // inherited from CrudRepository (like save/deleteById) - those get
        // an implicit transaction from SimpleJpaRepository's class-level
        // @Transactional, but a custom derived delete does not and needs
        // its own, or it fails at runtime with "No EntityManager with
        // actual transaction available for current thread".
        if (!favoriteQueryRepository.existsByIdAndUserId(favoriteId, userId)) {
            throw new ResourceNotFoundException("No favorite found with that id");
        }
        favoriteQueryRepository.deleteByIdAndUserId(favoriteId, userId);
    }
}
