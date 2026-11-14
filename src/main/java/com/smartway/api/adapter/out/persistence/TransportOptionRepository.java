package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.TransportOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransportOptionRepository extends JpaRepository<TransportOption, Long> {
    List<TransportOption> findByConsultationIdOrderByRankingAsc(Long consultationId);
}
