package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.ContextPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ContextPatternRepository extends JpaRepository<ContextPattern, Long> {
    List<ContextPattern> findByUserIdOrderByOccurrenceCountDesc(Long userId);
    Optional<ContextPattern> findByUserIdAndDestinationTypeAndTimeOfDayAndDayType(Long userId, String destinationType, String timeOfDay, String dayType);
}
