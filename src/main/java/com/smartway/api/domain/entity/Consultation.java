package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "origin_address", nullable = false)
    private String originAddress;

    @Column(name = "origin_lat")
    private double originLat;

    @Column(name = "origin_lng")
    private double originLng;

    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;

    @Column(name = "destination_lat")
    private double destinationLat;

    @Column(name = "destination_lng")
    private double destinationLng;

    @Column(name = "destination_type")
    private String destinationType;

    @Column(name = "trip_intent")
    private String tripIntent;

    @Column(name = "weather_condition")
    private String weatherCondition;

    @Column(name = "weather_temp")
    private Double weatherTemp;

    @Column(name = "weather_rain_probability")
    private Double weatherRainProbability;

    @Column(name = "traffic_level")
    private String trafficLevel;

    @Column(name = "traffic_delay_minutes")
    private Integer trafficDelayMinutes;

    @Column(name = "context_insights", columnDefinition = "jsonb")
    private String contextInsights;

    @OneToOne(mappedBy = "consultation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Recommendation recommendation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
