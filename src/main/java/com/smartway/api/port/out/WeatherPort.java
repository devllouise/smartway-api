package com.smartway.api.port.out;

import com.smartway.api.domain.model.WeatherInfo;

public interface WeatherPort {
    WeatherInfo getWeather(double latitude, double longitude);
}
