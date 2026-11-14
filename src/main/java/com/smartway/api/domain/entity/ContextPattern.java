package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "context_patterns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextPattern {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "destination_type", nullable = false)
    private String destinationType;
    @Column(name = "time_of_day")
    private String timeOfDay;
    @Column(name = "day_type")
    private String dayType;
    @Column(name = "most_chosen_mode")
    private String mostChosenMode;
    @Column(name = "occurrence_count")
    private int occurrenceCount;
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
}
