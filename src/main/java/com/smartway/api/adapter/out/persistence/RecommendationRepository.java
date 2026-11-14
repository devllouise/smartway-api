package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findByConsultationId(Long consultationId);
}
