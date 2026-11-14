package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndRecommendationHelpful(Long userId, boolean helpful);

    @Query("SELECT f.chosenMode, COUNT(f) FROM Feedback f WHERE f.user.id = :userId AND f.chosenMode IS NOT NULL GROUP BY f.chosenMode ORDER BY COUNT(f) DESC")
    List<Object[]> findMostChosenModesByUserId(Long userId);
}
