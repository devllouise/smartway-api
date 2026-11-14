package com.smartway.api.domain.model;

import com.smartway.api.domain.enums.TransportMode;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScoredOption {
    private TransportMode mode;
    private BigDecimal estimatedCost;
    private int estimatedTimeMinutes;
    private double distanceKm;
    private double comfortScore;
    private double safetyScore;
    private double ecoScore;
    private double finalScore;
    private int ranking;
    private String description;
    private Map<String, Object> costBreakdown;
    private boolean contextPenalized;
    private boolean contextBoosted;
    private boolean hardRestricted;
}
