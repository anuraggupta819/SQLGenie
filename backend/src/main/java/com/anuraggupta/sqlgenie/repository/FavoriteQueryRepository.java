package com.anuraggupta.sqlgenie.repository;

import com.anuraggupta.sqlgenie.entity.FavoriteQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoriteQueryRepository extends JpaRepository<FavoriteQuery, UUID> {

    List<FavoriteQuery> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
