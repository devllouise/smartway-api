package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Feedback do usuario sobre uma recomendacao do historico (um por busca). */
@Entity
@Table(name = "route_feedbacks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "history_id", nullable = false)
    private Long historyId;

    private Boolean helpful;

    @Column(name = "chosen_mode")
    private String chosenMode;

    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
