package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_consumption")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VehicleConsumption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String brand;
    private String model;
    private String version;
    @Column(name = "model_year")
    private Integer modelYear;
    private String fuel;
    @Column(name = "city_km_per_liter")
    private Double cityKmPerLiter;
    @Column(name = "highway_km_per_liter")
    private Double highwayKmPerLiter;
    private String source;
}
