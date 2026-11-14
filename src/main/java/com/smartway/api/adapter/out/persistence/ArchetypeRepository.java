package com.smartway.api.adapter.out.persistence;

import com.smartway.api.domain.entity.Archetype;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ArchetypeRepository extends JpaRepository<Archetype, Long> {
    Optional<Archetype> findByName(String name);
}
