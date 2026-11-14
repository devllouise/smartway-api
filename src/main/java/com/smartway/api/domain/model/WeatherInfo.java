package com.smartway.api.domain.model;

import com.smartway.api.domain.enums.WeatherCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfo {

    private double temperatureCelsius;
    private double temperature;
    private int humidity;
    private double rainProbability;
    private WeatherCondition condition;
    private String description;
    private double windSpeedKmh;
    private boolean isDayTime;
    private boolean available;

    public boolean isRainy() {
        return condition == WeatherCondition.RAIN
            || condition == WeatherCondition.LIGHT_RAIN
            || condition == WeatherCondition.HEAVY_RAIN
            || condition == WeatherCondition.STORM;
    }

    public boolean isDangerous() {
        return condition == WeatherCondition.STORM;
    }

    public boolean isExtreme() {
        return condition == WeatherCondition.STORM || condition == WeatherCondition.EXTREME_HEAT;
    }

    public boolean isCold() {
        return condition == WeatherCondition.COLD || temperatureCelsius <= 10.0;
    }

    public boolean isHot() {
        return condition == WeatherCondition.EXTREME_HEAT || temperatureCelsius >= 35.0;
    }
}