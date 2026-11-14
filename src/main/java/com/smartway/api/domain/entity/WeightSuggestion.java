package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Sugestao de ajuste de pesos gerada pelo aprendizado. So e aplicada se o usuario aceitar. */
@Entity
@Table(name = "weight_suggestions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** COST ou TIME */
    private String focus;

    @Column(name = "cost_weight")
    private double costWeight;
    @Column(name = "time_weight")
    private double timeWeight;
    @Column(name = "comfort_weight")
    private double comfortWeight;
    @Column(name = "safety_weight")
    private double safetyWeight;
    @Column(name = "eco_weight")
    private double ecoWeight;

    private String reason;

    /** PENDING, ACCEPTED ou REJECTED */
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
