package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.WeightSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeightSuggestionRepository extends JpaRepository<WeightSuggestion, Long> {
    Optional<WeightSuggestion> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    Optional<WeightSuggestion> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndStatus(Long userId, String status);
}
