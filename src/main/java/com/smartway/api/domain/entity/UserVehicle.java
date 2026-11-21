package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vehicles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserVehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;
    @Column(name = "fipe_brand_code")
    private String fipeBrandCode;
    @Column(name = "brand_name")
    private String brandName;
    @Column(name = "fipe_model_code")
    private String fipeModelCode;
    @Column(name = "model_name")
    private String modelName;
    @Column(name = "fipe_year_code")
    private String fipeYearCode;
    @Column(name = "model_year")
    private Integer modelYear;
    private String fuel;
    @Column(name = "km_per_liter", nullable = false)
    private double kmPerLiter;
    @Column(name = "km_per_liter_source", nullable = false)
    private String kmPerLiterSource;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }
}
