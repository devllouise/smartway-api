package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_places")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavedPlace {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    private String name;
    private String address;
    private String category;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
