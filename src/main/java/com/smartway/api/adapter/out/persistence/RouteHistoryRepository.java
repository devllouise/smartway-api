package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.RouteHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteHistoryRepository extends JpaRepository<RouteHistory, Long> {
    List<RouteHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<RouteHistory> findByIdAndUserId(Long id, Long userId);
}
