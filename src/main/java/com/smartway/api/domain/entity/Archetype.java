package com.smartway.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "archetypes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Archetype {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
    @Column(name = "default_cost_weight")
    private double defaultCostWeight;
    @Column(name = "default_time_weight")
    private double defaultTimeWeight;
    @Column(name = "default_comfort_weight")
    private double defaultComfortWeight;
    @Column(name = "default_safety_weight")
    private double defaultSafetyWeight;
    @Column(name = "default_eco_weight")
    private double defaultEcoWeight;
}
