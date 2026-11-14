package com.smartway.api.domain.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripContext {
    private String originAddress;
    private double originLat;
    private double originLng;
    private String destinationAddress;
    private double destinationLat;
    private double destinationLng;
    private double distanceKm;
    private int baseTimeMinutes;
    private WeatherInfo weather;
    private TrafficInfo traffic;
}
