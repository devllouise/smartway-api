package com.smartway.api.application.service;

import com.smartway.api.domain.enums.*;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import com.smartway.api.domain.model.WeatherInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ContextInsightGenerator {

    public List<ContextInsight> generate(ContextAnalysisResult context) {
        List<ContextInsight> insights = new ArrayList<>();

        generateWeatherInsights(context, insights);
        generateTrafficInsights(context, insights);
        generateDestinationInsights(context, insights);
        generateDistanceInsights(context, insights);
        generateTimeInsights(context, insights);
        generateComboInsights(context, insights);

        log.info("Gerados {} insights contextuais", insights.size());
        return insights;
    }

    // ===== WEATHER =====
    private void generateWeatherInsights(ContextAnalysisResult context, List<ContextInsight> insights) {
        WeatherInfo weather = context.getWeatherInfo();
        if (weather == null || !weather.isAvailable()) return;

        WeatherCondition condition = context.getWeather();

        if (condition == WeatherCondition.STORM) {
            insights.add(ContextInsight.builder()
                .id("STORM_ALERT").severity("CRITICAL").category("WEATHER")
                .title("Tempestade na regiao!")
                .message("Tempestade detectada! Se puder, adie a viagem. Se precisar sair, va de carro ou Uber.")
                .penalizedModes(List.of(TransportMode.MOTORCYCLE, TransportMode.BICYCLE, TransportMode.WALKING))
                .boostedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .penaltyFactor(0.1).boostFactor(1.5).hardRestriction(true)
                .build());
        } else if (condition == WeatherCondition.HEAVY_RAIN) {
            insights.add(ContextInsight.builder()
                .id("HEAVY_RAIN_WARNING").severity("WARNING").category("WEATHER")
                .title("Chuva forte")
                .message("Chuva forte na regiao! Evite moto e bicicleta. Onibus ou Uber sao mais seguros.")
                .penalizedModes(List.of(TransportMode.MOTORCYCLE, TransportMode.BICYCLE, TransportMode.WALKING))
                .boostedModes(List.of(TransportMode.BUS, TransportMode.UBER))
                .penaltyFactor(0.3).boostFactor(1.4).hardRestriction(false)
                .build());
        } else if (condition == WeatherCondition.RAIN || condition == WeatherCondition.LIGHT_RAIN) {
            insights.add(ContextInsight.builder()
                .id("LIGHT_RAIN_TIP").severity("INFO").category("WEATHER")
                .title("Chuvinha leve")
                .message("Chuva leve na regiao. Se for de moto, use capa. Onibus e uma boa opcao tambem!")
                .penalizedModes(List.of(TransportMode.MOTORCYCLE, TransportMode.BICYCLE))
                .boostedModes(List.of(TransportMode.BUS))
                .penaltyFactor(0.7).boostFactor(1.2).hardRestriction(false)
                .build());
        }

        if (weather.getTemperatureCelsius() >= 35.0) {
            insights.add(ContextInsight.builder()
                .id("EXTREME_HEAT").severity("WARNING").category("WEATHER")
                .title("Calor extremo")
                .message("Temperatura acima de 35C! Evite caminhar ou pedalar. Prefira transporte com ar-condicionado.")
                .penalizedModes(List.of(TransportMode.WALKING, TransportMode.BICYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.BUS))
                .penaltyFactor(0.4).boostFactor(1.3).hardRestriction(false)
                .build());
        }

        if (weather.getTemperatureCelsius() <= 10.0) {
            insights.add(ContextInsight.builder()
                .id("COLD_WEATHER").severity("INFO").category("WEATHER")
                .title("Frio intenso")
                .message("Esta bem frio! Se for de moto ou bicicleta, agasalhe-se bem.")
                .penalizedModes(List.of(TransportMode.MOTORCYCLE, TransportMode.BICYCLE))
                .boostedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .penaltyFactor(0.7).boostFactor(1.1).hardRestriction(false)
                .build());
        }
    }

    // ===== TRAFFIC =====
    private void generateTrafficInsights(ContextAnalysisResult context, List<ContextInsight> insights) {
        TrafficLevel traffic = context.getTrafficLevel();
        if (traffic == null) return;

        if (traffic == TrafficLevel.VERY_HIGH) {
            insights.add(ContextInsight.builder()
                .id("VERY_HIGH_TRAFFIC").severity("WARNING").category("TRAFFIC")
                .title("Transito parado!")
                .message("Transito muito pesado agora! Moto fura o transito, onibus tem faixa exclusiva. Carro vai demorar.")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .boostedModes(List.of(TransportMode.MOTORCYCLE, TransportMode.BUS, TransportMode.TRAIN))
                .penaltyFactor(0.5).boostFactor(1.4).hardRestriction(false)
                .build());
        } else if (traffic == TrafficLevel.HIGH) {
            insights.add(ContextInsight.builder()
                .id("HIGH_TRAFFIC").severity("INFO").category("TRAFFIC")
                .title("Transito moderado")
                .message("Transito um pouco pesado. Considere transporte publico pra evitar estresse.")
                .penalizedModes(List.of(TransportMode.CAR))
                .boostedModes(List.of(TransportMode.BUS, TransportMode.TRAIN))
                .penaltyFactor(0.7).boostFactor(1.2).hardRestriction(false)
                .build());
        }
    }

    // ===== DESTINATION =====
    private void generateDestinationInsights(ContextAnalysisResult context, List<ContextInsight> insights) {
        DestinationType dest = context.getDestinationType();
        if (dest == null) return;

        if (dest == DestinationType.BAR) {
            insights.add(ContextInsight.builder()
                .id("BAR_DUI").severity("CRITICAL").category("SAFETY")
                .title("Lei Seca!")
                .message("Indo pro bar? Nao dirija se for beber! Use Uber ou transporte publico. Sua seguranca em primeiro lugar.")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.MOTORCYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.BUS))
                .penaltyFactor(0.1).boostFactor(1.5).hardRestriction(true)
                .build());
        }

        if (dest == DestinationType.HOSPITAL) {
            insights.add(ContextInsight.builder()
                .id("HOSPITAL_FAST").severity("CRITICAL").category("DESTINATION")
                .title("Destino hospitalar")
                .message("Indo ao hospital? Priorize o meio mais rapido! Em emergencia, considere chamar SAMU (192).")
                .penalizedModes(List.of(TransportMode.BUS, TransportMode.WALKING, TransportMode.BICYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.CAR))
                .penaltyFactor(0.2).boostFactor(1.5).hardRestriction(false)
                .build());
        }

        if (dest == DestinationType.SHOPPING_MALL) {
            insights.add(ContextInsight.builder()
                .id("SHOPPING_PARKING").severity("INFO").category("DESTINATION")
                .title("Estacionamento de shopping")
                .message("Shoppings costumam ter estacionamento pago. Onibus ou Uber evitam essa dor de cabeca!")
                .penalizedModes(List.of(TransportMode.CAR))
                .boostedModes(List.of(TransportMode.BUS, TransportMode.UBER))
                .penaltyFactor(0.8).boostFactor(1.1).hardRestriction(false)
                .build());
        }

        if (dest == DestinationType.UNIVERSITY) {
            insights.add(ContextInsight.builder()
                .id("UNIVERSITY_TIP").severity("INFO").category("DESTINATION")
                .title("Indo pra faculdade")
                .message("Estudante? Onibus com passe estudantil e a opcao mais economica!")
                .penalizedModes(List.of())
                .boostedModes(List.of(TransportMode.BUS))
                .penaltyFactor(1.0).boostFactor(1.3).hardRestriction(false)
                .build());
        }

        if (dest == DestinationType.PARK) {
            insights.add(ContextInsight.builder()
                .id("PARK_ACTIVE").severity("INFO").category("DESTINATION")
                .title("Destino ao ar livre")
                .message("Indo pro parque ou praia? Que tal ir de bike ou a pe pra ja comecar o exercicio?")
                .penalizedModes(List.of())
                .boostedModes(List.of(TransportMode.BICYCLE, TransportMode.WALKING))
                .penaltyFactor(1.0).boostFactor(1.3).hardRestriction(false)
                .build());
        }

        if (dest == DestinationType.AIRPORT) {
            insights.add(ContextInsight.builder()
                .id("AIRPORT_COST").severity("WARNING").category("DESTINATION")
                .title("Estacionamento de aeroporto")
                .message("Aeroporto tem estacionamento caríssimo! Uber ou trem sao muito mais em conta.")
                .penalizedModes(List.of(TransportMode.CAR))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.TRAIN))
                .penaltyFactor(0.4).boostFactor(1.4).hardRestriction(false)
                .build());
        }
    }

    // ===== DISTANCE =====
    private void generateDistanceInsights(ContextAnalysisResult context, List<ContextInsight> insights) {
        double distance = context.getDistanceKm();

        if (distance <= 1.0) {
            insights.add(ContextInsight.builder()
                .id("VERY_SHORT").severity("INFO").category("DISTANCE")
                .title("Pertinho!")
                .message("Menos de 1km! Va a pe, e rapido e de graca!")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .boostedModes(List.of(TransportMode.WALKING))
                .penaltyFactor(0.5).boostFactor(1.5).hardRestriction(false)
                .build());
        } else if (distance <= 3.0) {
            insights.add(ContextInsight.builder()
                .id("SHORT_DISTANCE").severity("INFO").category("DISTANCE")
                .title("Distancia curta")
                .message("Distancia curta! Bicicleta ou caminhada sao otimas opcoes.")
                .penalizedModes(List.of(TransportMode.CAR))
                .boostedModes(List.of(TransportMode.WALKING, TransportMode.BICYCLE))
                .penaltyFactor(0.7).boostFactor(1.3).hardRestriction(false)
                .build());
        } else if (distance > 30.0) {
            insights.add(ContextInsight.builder()
                .id("LONG_DISTANCE").severity("INFO").category("DISTANCE")
                .title("Viagem longa")
                .message("Distancia grande! Carro ou trem sao mais confortaveis pra viagens longas.")
                .penalizedModes(List.of(TransportMode.BICYCLE, TransportMode.WALKING))
                .boostedModes(List.of(TransportMode.CAR, TransportMode.UBER))
                .penaltyFactor(0.2).boostFactor(1.3).hardRestriction(false)
                .build());
        }
    }

    // ===== TIME =====
    private void generateTimeInsights(ContextAnalysisResult context, List<ContextInsight> insights) {
        TimeOfDay time = context.getTimeOfDay();
        if (time == null) return;

        if (time == TimeOfDay.LATE_NIGHT) {
            insights.add(ContextInsight.builder()
                .id("LATE_NIGHT").severity("WARNING").category("SAFETY")
                .title("Madrugada")
                .message("Madrugada! Evite andar a pe ou de bike. Uber ou carro sao mais seguros.")
                .penalizedModes(List.of(TransportMode.WALKING, TransportMode.BICYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.CAR))
                .penaltyFactor(0.3).boostFactor(1.4).hardRestriction(false)
                .build());
        }
    }

    // ===== COMBOS =====
    private void generateComboInsights(ContextAnalysisResult context, List<ContextInsight> insights) {
        WeatherInfo weather = context.getWeatherInfo();
        TrafficLevel traffic = context.getTrafficLevel();
        DestinationType dest = context.getDestinationType();

        // Chuva + Moto
        if (weather != null && weather.isRainy()) {
            insights.add(ContextInsight.builder()
                .id("RAIN_MOTORCYCLE").severity("WARNING").category("COMBO")
                .title("Chuva + Moto = Perigo")
                .message("Chovendo e voce considera moto? Pista molhada e perigosa! Use capa ou prefira Uber/onibus.")
                .penalizedModes(List.of(TransportMode.MOTORCYCLE))
                .boostedModes(List.of(TransportMode.UBER, TransportMode.BUS))
                .penaltyFactor(0.3).boostFactor(1.3).hardRestriction(false)
                .build());
        }

        // Noite + Bar
        if (context.getTimeOfDay() == TimeOfDay.NIGHT && dest == DestinationType.BAR) {
            insights.add(ContextInsight.builder()
                .id("NIGHT_BAR_COMBO").severity("CRITICAL").category("COMBO")
                .title("Noite + Bar")
                .message("Noite no bar? Nao dirija! Combine um Uber ou va de transporte publico. Volte seguro!")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.MOTORCYCLE))
                .boostedModes(List.of(TransportMode.UBER))
                .penaltyFactor(0.05).boostFactor(1.5).hardRestriction(true)
                .build());
        }

        // Transito pesado + Chuva
        if (traffic == TrafficLevel.VERY_HIGH && weather != null && weather.isRainy()) {
            insights.add(ContextInsight.builder()
                .id("TRAFFIC_RAIN_COMBO").severity("WARNING").category("COMBO")
                .title("Transito + Chuva")
                .message("Transito pesado E chovendo! Dia complicado. Onibus com faixa exclusiva pode ser a melhor opcao.")
                .penalizedModes(List.of(TransportMode.CAR, TransportMode.MOTORCYCLE))
                .boostedModes(List.of(TransportMode.BUS, TransportMode.TRAIN))
                .penaltyFactor(0.4).boostFactor(1.4).hardRestriction(false)
                .build());
        }
    }
}