package com.anuraggupta.sqlgenie.repository;

import com.anuraggupta.sqlgenie.entity.QueryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, UUID> {

    Page<QueryHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<QueryHistory> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
