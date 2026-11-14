package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "preference_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
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
    @Column(name = "trigger_reason")
    private String triggerReason;
    private int version;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
