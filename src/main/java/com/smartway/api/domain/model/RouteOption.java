package com.smartway.api.domain.model;

import com.smartway.api.domain.enums.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteOption {

    private TransportMode mode;
    private double score;
    private double estimatedCost;
    private int estimatedTimeMinutes;
    private double distanceKm;
    private String details;
}