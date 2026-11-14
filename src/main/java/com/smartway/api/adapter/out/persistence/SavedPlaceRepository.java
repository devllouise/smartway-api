package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.SavedPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {
    List<SavedPlace> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavedPlace> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndAddressIgnoreCase(Long userId, String address);

    @Query(value = "SELECT destination FROM route_history WHERE user_id = :userId "
        + "GROUP BY destination ORDER BY MAX(created_at) DESC LIMIT 10", nativeQuery = true)
    List<String> recentDestinations(Long userId);
}
