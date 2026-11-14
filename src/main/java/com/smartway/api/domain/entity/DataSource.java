package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(name = "source_type", nullable = false)
    private String sourceType;
    @Column(name = "base_url")
    private String baseUrl;
    @Column(name = "is_active")
    private boolean isActive;
    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;
    @Column(name = "failure_count")
    private int failureCount;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
