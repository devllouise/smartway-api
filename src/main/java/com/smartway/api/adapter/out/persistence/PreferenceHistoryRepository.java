package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.PreferenceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PreferenceHistoryRepository extends JpaRepository<PreferenceHistory, Long> {
    List<PreferenceHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
