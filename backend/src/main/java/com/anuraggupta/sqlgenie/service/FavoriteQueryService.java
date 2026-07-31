package com.anuraggupta.sqlgenie.service;

import com.anuraggupta.sqlgenie.dto.request.FavoriteRequest;
import com.anuraggupta.sqlgenie.dto.response.FavoriteResponse;

import java.util.List;
import java.util.UUID;

public interface FavoriteQueryService {

    FavoriteResponse save(UUID userId, FavoriteRequest request);

    List<FavoriteResponse> getFavorites(UUID userId);

    void deleteFavorite(UUID userId, UUID favoriteId);
}
