package com.smartway.api.application.service;

import com.smartway.api.adapter.in.dto.request.RecommendationRequest;
import com.smartway.api.domain.enums.*;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import com.smartway.api.domain.model.WeatherInfo;
import com.smartway.api.port.out.PlacesPort;
import com.smartway.api.port.out.TrafficPort;
import com.smartway.api.port.out.WeatherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextAnalyzerService {

    private final WeatherPort weatherPort;
    private final TrafficPort trafficPort;
    private final PlacesPort placesPort;

    public ContextAnalysisResult analyze(RecommendationRequest request) {
        WeatherInfo weather = weatherPort.getWeather(-23.96, -46.40);
        TrafficPort.RouteInfo routeInfo = trafficPort.getRouteInfo(request.getOrigin(), request.getDestination());
        TrafficLevel traffic = routeInfo.trafficLevel();
        DestinationType destType = placesPort.getDestinationType(request.getDestination());
        double distance = routeInfo.distanceKm();
        if (routeInfo.estimated()) log.warn("Distancia estimada (Routes indisponivel): {} km", distance);
        TimeOfDay timeOfDay = determineTimeOfDay();
        String dayType = determineDayType();

        ContextAnalysisResult context = ContextAnalysisResult.builder()
            .weather(weather.getCondition())
            .temperatureCelsius(weather.getTemperatureCelsius())
            .timeOfDay(timeOfDay)
            .dayType(dayType)
            .trafficLevel(traffic)
            .destinationType(destType)
            .distanceKm(distance)
            .intent(inferIntent(destType))
            .weatherInfo(weather)
            .build();

        generateInsights(context, weather, traffic, destType, distance, timeOfDay);
        return context;
    }

    private void generateInsights(ContextAnalysisResult context, WeatherInfo weather,
            TrafficLevel traffic, DestinationType destType, double distance, TimeOfDay timeOfDay) {

        if (weather.isRainy()) {
            context.addInsight(ContextInsight.builder()
                .id("RAIN_WARNING").severity("WARNING").category("WEATHER")
                .title("Chuva detectada")
                .message("Esta chovendo na regiao! Bike, moto e caminhada ficam arriscadas; prefira Uber, carro ou onibus.")
                .penalizedModes(List.of(TransportMode.MOTORCYCLE, TransportMode.BICYCLE, TransportMode.WALKING))
                .boostedModes(List.of(TransportMode.BUS, TransportMode.UBER, TransportMode.NINETY_NINE, TransportMode.CAR))
                .penaltyFactor(0.3).boostFactor(1.5).hardRestriction(false)
                .build());
        }

        if (weather.isDangerous() || weather.getCondition() == com.smartway.api.domain.enums.WeatherCondition.HEAVY_RAIN) {
            context.addInsight(ContextInsight.builder()
                .id("STORM_BLOCK").severity("CRITICAL").category("WEATHER")
                .title("Chuva forte ou tempestade")
                .message("Chuva forte na regiao! Bike e caminhada nao sao seguras agora; prefira Uber, carro ou onibus.")
                .penalizedModes(List.of(TransportMode.BICYCLE, TransportMode.WALKING))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.NINETY_NINE, TransportMode.CAR))
                .penaltyFactor(0.1).boostFactor(1.5).hardRestriction(true)
                .build());
            context.addInsight(ContextInsight.builder()
                .id("STORM_MOTORCYCLE").severity("WARNING").category("WEATHER")
                .title("Moto na chuva forte")
                .message("Moto com chuva forte tem pista escorregadia e pouca visibilidade.")
                .penalizedModes(List.of(TransportMode.MOTORCYCLE))
                .boostedModes(List.of())
                .penaltyFactor(0.2).boostFactor(1.0).hardRestriction(false)
                .build());
        }

        if (weather.getTemperatureCelsius() >= 35.0) {
            context.addInsight(ContextInsight.builder()
                .id("EXTREME_HEAT").severity("WARNING").category("WEATHER")
                .title("Calor extremo")
                .message("Temperatura acima de 35C! Evite caminhar ou pedalar longas distancias.")
                .penalizedModes(List.of(TransportMode.WALKING, TransportMode.BICYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.BUS))
                .penaltyFactor(0.4).boostFactor(1.2).hardRestriction(false)
                .build());
        }

        if (traffic == TrafficLevel.VERY_HIGH) {
            context.addInsight(ContextInsight.builder()
                .id("HEAVY_TRAFFIC").severity("WARNING").category("TRAFFIC")
                .title("Transito intenso")
                .message("Transito muito pesado! Considere transporte publico ou moto.")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .boostedModes(List.of(TransportMode.MOTORCYCLE, TransportMode.BUS, TransportMode.TRAIN))
                .penaltyFactor(0.6).boostFactor(1.3).hardRestriction(false)
                .build());
        }

        if (destType == DestinationType.BAR) {
            context.addInsight(ContextInsight.builder()
                .id("BAR_DUI_WARNING").severity("CRITICAL").category("SAFETY")
                .title("Lei Seca")
                .message("Voce esta indo para um bar! Nao dirija se for beber. Use Uber ou transporte publico.")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.MOTORCYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.BUS))
                .penaltyFactor(0.1).boostFactor(1.5).hardRestriction(true)
                .build());
        }

        if (destType == DestinationType.AIRPORT) {
            context.addInsight(ContextInsight.builder()
                .id("AIRPORT_PARKING").severity("WARNING").category("DESTINATION")
                .title("Estacionamento caro")
                .message("Aeroportos tem estacionamento caro! Considere Uber ou trem.")
                .penalizedModes(List.of(TransportMode.CAR))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.TRAIN))
                .penaltyFactor(0.5).boostFactor(1.3).hardRestriction(false)
                .build());
        }

        if (destType == DestinationType.HOSPITAL) {
            context.addInsight(ContextInsight.builder()
                .id("HOSPITAL_URGENCY").severity("CRITICAL").category("DESTINATION")
                .title("Destino hospitalar")
                .message("Indo ao hospital? Priorize o meio mais rapido!")
                .penalizedModes(List.of(TransportMode.BUS, TransportMode.WALKING))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.CAR))
                .penaltyFactor(0.3).boostFactor(1.5).hardRestriction(false)
                .build());
        }

        if (distance <= 1.5) {
            context.addInsight(ContextInsight.builder()
                .id("SHORT_DISTANCE").severity("INFO").category("DISTANCE")
                .title("Pertinho!")
                .message("Distancia curta! Que tal ir a pe ou de bicicleta?")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .boostedModes(List.of(TransportMode.WALKING, TransportMode.BICYCLE))
                .penaltyFactor(0.7).boostFactor(1.5).hardRestriction(false)
                .build());
        }

        if (distance > 30.0) {
            context.addInsight(ContextInsight.builder()
                .id("LONG_DISTANCE").severity("INFO").category("DISTANCE")
                .title("Viagem longa")
                .message("Distancia grande! Carro ou trem podem ser mais confortaveis.")
                .penalizedModes(List.of(TransportMode.BICYCLE, TransportMode.WALKING))
                .boostedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .penaltyFactor(0.2).boostFactor(1.3).hardRestriction(false)
                .build());
        }

        if (timeOfDay == TimeOfDay.NIGHT) {
            context.addInsight(ContextInsight.builder()
                .id("NIGHT_VISIBILITY").severity("INFO").category("SAFETY")
                .title("Periodo noturno")
                .message("Ja esta escuro: se for de bike ou a pe, use luz e prefira vias iluminadas.")
                .penalizedModes(List.of(TransportMode.WALKING, TransportMode.BICYCLE))
                .boostedModes(List.of())
                .penaltyFactor(0.8).boostFactor(1.0).hardRestriction(false)
                .build());
        }

        if (timeOfDay == TimeOfDay.LATE_NIGHT) {
            context.addInsight(ContextInsight.builder()
                .id("LATE_NIGHT_SAFETY").severity("WARNING").category("SAFETY")
                .title("Horario noturno")
                .message("Madrugada! Evite caminhar ou pedalar. Prefira Uber ou carro.")
                .penalizedModes(List.of(TransportMode.WALKING, TransportMode.BICYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.CAR))
                .penaltyFactor(0.3).boostFactor(1.4).hardRestriction(false)
                .build());
        }
    }

    private TimeOfDay determineTimeOfDay() {
        int hour = LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).getHour();
        if (hour >= 5 && hour < 12) return TimeOfDay.MORNING;
        if (hour >= 12 && hour < 18) return TimeOfDay.AFTERNOON;
        if (hour >= 18) return TimeOfDay.NIGHT;
        return TimeOfDay.LATE_NIGHT;
    }

    private String determineDayType() {
        int day = LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).getDayOfWeek().getValue();
        return (day >= 6) ? "WEEKEND" : "WEEKDAY";
    }

    private String inferIntent(DestinationType destType) {
        return switch (destType) {
            case BAR, RESTAURANT -> "LEISURE";
            case WORK -> "COMMUTE";
            case UNIVERSITY -> "STUDY";
            case HOSPITAL -> "EMERGENCY";
            case AIRPORT -> "TRAVEL";
            default -> "GENERAL";
        };
    }

    private double estimateDistance(String origin, String destination) {
        return 8.5;
    }
}