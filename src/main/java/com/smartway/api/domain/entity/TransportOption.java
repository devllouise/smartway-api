package com.smartway.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "transport_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;
    @Column(nullable = false)
    private String mode;
    @Column(name = "estimated_cost", nullable = false)
    private BigDecimal estimatedCost;
    @Column(name = "estimated_time_minutes", nullable = false)
    private int estimatedTimeMinutes;
    @Column(name = "distance_km")
    private double distanceKm;
    @Column(name = "comfort_score")
    private double comfortScore;
    @Column(name = "safety_score")
    private double safetyScore;
    @Column(name = "eco_score")
    private double ecoScore;
    @Column(name = "final_score")
    private double finalScore;
    private int ranking;
    private String description;
    @Column(name = "cost_breakdown", columnDefinition = "jsonb")
    private String costBreakdown;
    @Column(name = "context_penalized")
    private boolean contextPenalized;
    @Column(name = "context_boosted")
    private boolean contextBoosted;
}
