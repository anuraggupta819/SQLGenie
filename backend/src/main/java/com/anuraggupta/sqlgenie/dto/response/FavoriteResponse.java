package com.anuraggupta.sqlgenie.dto.response;

import com.anuraggupta.sqlgenie.entity.FavoriteQuery;

import java.time.Instant;
import java.util.UUID;

public record FavoriteResponse(
        UUID id,
        String name,
        String naturalLanguageQuery,
        String generatedSql,
        String explanation,
        Instant createdAt
) {

    public static FavoriteResponse from(FavoriteQuery entity, String explanation) {
        return new FavoriteResponse(
                entity.getId(),
                entity.getName(),
                entity.getNaturalLanguageQuery(),
                entity.getGeneratedSql(),
                explanation,
                entity.getCreatedAt()
        );
    }
}
