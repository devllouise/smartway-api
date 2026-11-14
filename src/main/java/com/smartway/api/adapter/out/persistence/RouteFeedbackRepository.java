package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.RouteFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteFeedbackRepository extends JpaRepository<RouteFeedback, Long> {

    Optional<RouteFeedback> findByHistoryId(Long historyId);

    List<RouteFeedback> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    /** Modal que o usuario mais escolheu nesse tipo de destino e horario (minimo de ocorrencias). */
    @Query(value = "SELECT f.chosen_mode FROM route_feedbacks f JOIN route_history h ON h.id = f.history_id "
        + "WHERE f.user_id = :userId AND h.destination_type = :dest AND h.time_of_day = :tod "
        + "AND f.chosen_mode IS NOT NULL GROUP BY f.chosen_mode HAVING COUNT(*) >= :min "
        + "ORDER BY COUNT(*) DESC LIMIT 1", nativeQuery = true)
    List<String> habitualMode(@Param("userId") Long userId, @Param("dest") String dest,
                              @Param("tod") String tod, @Param("min") long min);
}
