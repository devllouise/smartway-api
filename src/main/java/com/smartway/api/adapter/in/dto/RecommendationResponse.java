package com.smartway.api.adapter.in.dto;

import com.smartway.api.domain.enums.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private String requestId;
    private TransportMode recommendedMode;
    private String explanation;
    private List<ModeOption> options;
    private List<InsightResponse> insights;
    private ContextSummary context;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModeOption {
        private TransportMode mode;
        private double cost;
        private int timeMinutes;
        private double score;
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightResponse {
        private String id;
        private String severity;
        private String category;
        private String title;
        private String message;
        private TransportMode affectedMode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextSummary {
        private String weather;
        private double temperatureCelsius;
        private String timeOfDay;
        private String trafficLevel;
        private String destinationType;
        private double distanceKm;
    }
}
