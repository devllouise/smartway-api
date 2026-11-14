package com.smartway.api.adapter.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class RecommendationResponse {
    private RecommendedOption recommended;
    private String headline;
    private String explanation;
    private List<String> reasons;
    private List<AlternativeOption> alternatives;
    private Map<String, Double> weightsUsed;
    private boolean aiGenerated;

    @Data
    @Builder
    @AllArgsConstructor
    public static class RecommendedOption {
        private String mode;
        private BigDecimal estimatedCost;
        private int estimatedTimeMinutes;
        private double distanceKm;
        private double finalScore;
        private Map<String, Object> costBreakdown;
        private double comfortScore;
        private double safetyScore;
        private double ecoScore;
        private boolean contextBoosted;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class AlternativeOption {
        private String mode;
        private BigDecimal cost;
        private int time;
        private double score;
        private String warning;
    }
}
