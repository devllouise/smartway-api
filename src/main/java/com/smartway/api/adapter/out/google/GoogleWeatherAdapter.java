package com.smartway.api.adapter.out.google;

import com.smartway.api.domain.enums.WeatherCondition;
import com.smartway.api.domain.model.WeatherInfo;
import com.smartway.api.port.out.WeatherPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class GoogleWeatherAdapter implements WeatherPort {

    private static final String WEATHER_URL = "https://weather.googleapis.com/v1/currentConditions:lookup";

    @Value("${google.maps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleWeatherAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public WeatherInfo getWeather(double latitude, double longitude) {
        try {
            String url = String.format(java.util.Locale.US, "%s?location.latitude=%f&location.longitude=%f",
                WEATHER_URL, latitude, longitude);

            log.info("Consultando Google Weather API: lat={}, lon={}", latitude, longitude);
            org.springframework.http.HttpHeaders h = new org.springframework.http.HttpHeaders(); h.set("X-Goog-Api-Key", apiKey); String response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(h), String.class).getBody();
            JsonNode root = objectMapper.readTree(response);

            double tempCelsius = root.path("temperature").path("degrees").asDouble(25.0);
            int humidity = root.path("humidity").asInt(60);
            String description = root.path("weatherCondition").path("description").path("text").asText("Desconhecido");
            double windSpeed = root.path("wind").path("speed").path("value").asDouble(10.0);
            boolean isDayTime = root.path("isDayTime").asBoolean(true);
            double rainProb = root.path("precipitationProbability").asDouble(0.0);

            WeatherCondition condition = mapCondition(tempCelsius, description);

            WeatherInfo info = WeatherInfo.builder()
                .temperatureCelsius(tempCelsius)
                .temperature(tempCelsius)
                .humidity(humidity)
                .rainProbability(rainProb)
                .condition(condition)
                .description(description)
                .windSpeedKmh(windSpeed)
                .isDayTime(isDayTime)
                .available(true)
                .build();

            log.info("Clima: {}C, {}, {}", tempCelsius, condition, description);
            return info;

        } catch (Exception e) {
            log.error("Erro Google Weather API: {}", e.getMessage());
            return WeatherInfo.builder()
                .temperatureCelsius(25.0).temperature(25.0)
                .humidity(60).rainProbability(0.0)
                .condition(WeatherCondition.CLEAR)
                .description("Dados indisponiveis")
                .windSpeedKmh(10.0).isDayTime(true).available(false)
                .build();
        }
    }

    private WeatherCondition mapCondition(double temp, String desc) {
        String d = desc.toLowerCase();
        if (d.contains("thunder") || d.contains("storm") || d.contains("tempestade")) return WeatherCondition.STORM;
        if (d.contains("heavy rain") || d.contains("chuva forte")) return WeatherCondition.HEAVY_RAIN;
        if (d.contains("rain") || d.contains("drizzle") || d.contains("chuva")) return WeatherCondition.RAIN;
        if (d.contains("fog") || d.contains("mist") || d.contains("neblina")) return WeatherCondition.FOG;
        if (temp >= 35.0) return WeatherCondition.EXTREME_HEAT;
        if (temp <= 10.0) return WeatherCondition.COLD;
        if (d.contains("cloud") || d.contains("overcast") || d.contains("nublado")) return WeatherCondition.CLOUDY;
        return WeatherCondition.CLEAR;
    }
}