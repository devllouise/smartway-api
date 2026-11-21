package com.smartway.api.domain.model;

import com.smartway.api.domain.enums.DestinationType;
import com.smartway.api.domain.enums.TimeOfDay;
import com.smartway.api.domain.enums.TrafficLevel;
import com.smartway.api.domain.enums.WeatherCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextAnalysisResult {

    private WeatherCondition weather;
    private double temperatureCelsius;
    private TimeOfDay timeOfDay;
    private String dayType;
    private TrafficLevel trafficLevel;
    private DestinationType destinationType;
    private double distanceKm;
    private String intent;
    private WeatherInfo weatherInfo;
    private TransitRoute transit;
    /** Pedagio estimado do trajeto de carro (Google Routes), em reais. */
    private double tollCost;

    @Builder.Default
    private List<ContextInsight> insights = new ArrayList<>();

    public void addInsight(ContextInsight insight) {
        if (this.insights == null) {
            this.insights = new ArrayList<>();
        }
        this.insights.add(insight);
    }
}