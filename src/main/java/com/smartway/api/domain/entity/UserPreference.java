package com.smartway.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archetype_id")
    private Archetype archetype;
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
    private int version;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @jakarta.persistence.Column(name = "available_modes")
    private String availableModes;

    private String priorities;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
