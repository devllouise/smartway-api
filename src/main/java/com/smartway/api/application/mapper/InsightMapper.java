package com.smartway.api.application.mapper;

import com.smartway.api.adapter.in.dto.RecommendationResponse.ContextSummary;
import com.smartway.api.adapter.in.dto.RecommendationResponse.InsightResponse;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InsightMapper {

    public List<InsightResponse> toResponseList(List<ContextInsight> insights) {
        return insights.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public InsightResponse toResponse(ContextInsight insight) {
        return InsightResponse.builder()
                .id(insight.getId())
                .severity(insight.getSeverity())
                .category(insight.getCategory())
                .title(insight.getTitle())
                .message(insight.getMessage())
                .affectedMode(insight.getAffectedMode())
                .build();
    }

    public ContextSummary toContextSummary(ContextAnalysisResult ctx) {
        return ContextSummary.builder()
                .weather(ctx.getWeather() != null ? ctx.getWeather().name() : null)
                .temperatureCelsius(ctx.getTemperatureCelsius())
                .timeOfDay(ctx.getTimeOfDay() != null ? ctx.getTimeOfDay().name() : null)
                .trafficLevel(ctx.getTrafficLevel() != null ? ctx.getTrafficLevel().name() : null)
                .destinationType(ctx.getDestinationType() != null ? ctx.getDestinationType().name() : null)
                .distanceKm(ctx.getDistanceKm())
                .build();
    }
}
