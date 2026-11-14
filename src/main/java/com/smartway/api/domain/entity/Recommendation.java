package com.smartway.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    private Consultation consultation;
    @Column(name = "recommended_mode", nullable = false)
    private String recommendedMode;
    @Column(nullable = false)
    private String headline;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;
    @Column(nullable = false, columnDefinition = "jsonb")
    private String reasons;
    @Column(nullable = false, columnDefinition = "jsonb")
    private String alternatives;
    @Column(name = "context_insights", columnDefinition = "jsonb")
    private String contextInsights;
    @Column(name = "weights_used", nullable = false, columnDefinition = "jsonb")
    private String weightsUsed;
    @Column(name = "ai_generated")
    private boolean aiGenerated;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
