package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String origin;
    private String destination;

    @Column(name = "recommended_mode")
    private String recommendedMode;

    @Column(name = "recommended_label")
    private String recommendedLabel;

    @Column(name = "price_formatted")
    private String priceFormatted;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "bibi_message")
    private String bibiMessage;

    @Column(name = "card_json", columnDefinition = "TEXT")
    private String cardJson;

    // Contexto da busca: base do aprendizado (habito por tipo de destino e horario)
    @Column(name = "destination_type")
    private String destinationType;

    @Column(name = "time_of_day")
    private String timeOfDay;

    @Column(name = "day_type")
    private String dayType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"));
    }
}
