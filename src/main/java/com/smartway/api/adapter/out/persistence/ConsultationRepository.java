package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Page<Consultation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
