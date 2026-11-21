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
        TrafficPort.RouteInfo routeInfo = trafficPort.getRouteInfo(request.getOrigin(), request.getDestination());
        WeatherInfo weather = weatherPort.getWeather(routeInfo.originLat(), routeInfo.originLng());
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
            .tollCost(routeInfo.tollBrl())
            .build();

        generateInsights(context, weather, traffic, destType, distance, timeOfDay);
        return context;
    }

    private static final TransportMode CAR = TransportMode.CAR, MOTO = TransportMode.MOTORCYCLE,
        BUS = TransportMode.BUS, RIDE_SHARE = TransportMode.RIDE_SHARE,
        BIKE = TransportMode.BICYCLE, WALK = TransportMode.WALKING;

    /**
     * Motor de insights contextuais: cada regra identifica uma situacao (clima, horario, destino,
     * distancia, transito) e diz quais modais ficam mais arriscados ou mais vantajosos.
     * As regras nao presumem o comportamento do usuario: apenas descrevem o contexto.
     */
    private void generateInsights(ContextAnalysisResult c, WeatherInfo w,
            TrafficLevel traffic, DestinationType dest, double km, TimeOfDay tod) {

        int hour = LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).getHour();
        boolean weekend = "WEEKEND".equals(c.getDayType());
        boolean available = w.isAvailable();
        boolean heavyRain = available && (w.isDangerous() || w.getCondition() == WeatherCondition.HEAVY_RAIN);

        // ---------------- CLIMA ----------------
        if (heavyRain) {
            add(c, "STORM_BLOCK", "CRITICAL", "WEATHER", "Chuva forte ou tempestade",
                "Chuva forte na região: bicicleta e caminhada não são seguras agora.",
                "CHUVA FORTE", "DANGER", List.of(BIKE, WALK), List.of(RIDE_SHARE, CAR, BUS), 0.1, 1.4, true);
            add(c, "STORM_MOTORCYCLE", "WARNING", "WEATHER", "Moto na chuva forte",
                "Com chuva forte, a pista fica escorregadia e a visibilidade cai para quem está de moto.",
                "PISTA MOLHADA", "WEATHER", List.of(MOTO), List.of(), 0.2, 1.0, false);
        } else if (available && w.isRainy()) {
            add(c, "RAIN_WARNING", "WARNING", "WEATHER", "Chuva",
                "Está chovendo na região: moto, bicicleta e caminhada ficam mais arriscadas.",
                "PREVISÃO DE CHUVA", "WEATHER", List.of(MOTO, BIKE, WALK), List.of(BUS, RIDE_SHARE, CAR), 0.3, 1.5, false);
        } else if (available && w.getRainProbability() >= 60) {
            add(c, "RAIN_LIKELY", "INFO", "WEATHER", "Chance de chuva",
                "Há boa chance de chover durante o trajeto.",
                "CHANCE DE CHUVA", "WEATHER", List.of(MOTO, BIKE, WALK), List.of(), 0.75, 1.0, false);
        }
        if (available && w.getCondition() == WeatherCondition.FOG) {
            add(c, "FOG", "WARNING", "WEATHER", "Neblina",
                "Há neblina na região, o que reduz a visibilidade para moto e bicicleta.",
                "NEBLINA", "WEATHER", List.of(MOTO, BIKE), List.of(), 0.7, 1.0, false);
        }
        if (available && w.getWindSpeedKmh() >= 40) {
            add(c, "STRONG_WIND", "WARNING", "WEATHER", "Vento forte",
                "O vento está forte, o que deixa moto e bicicleta mais instáveis.",
                "VENTO FORTE", "WEATHER", List.of(MOTO, BIKE), List.of(), 0.7, 1.0, false);
        }
        if (available && w.getTemperatureCelsius() >= 35) {
            add(c, "EXTREME_HEAT", "WARNING", "WEATHER", "Calor intenso",
                "A temperatura passa de 35 °C: caminhar ou pedalar por muito tempo pode fazer mal.",
                "CALOR INTENSO", "WARNING", List.of(WALK, BIKE), List.of(BUS, RIDE_SHARE, CAR), 0.4, 1.2, false);
        } else if (available && w.getTemperatureCelsius() >= 30 && km > 2) {
            add(c, "HOT", "INFO", "WEATHER", "Dia quente",
                "Está quente: em trajetos mais longos, caminhar ou pedalar cansa mais.",
                "DIA QUENTE", "WARNING", List.of(WALK, BIKE), List.of(), 0.85, 1.0, false);
        } else if (available && w.getTemperatureCelsius() <= 12) {
            add(c, "COLD", "INFO", "WEATHER", "Frio",
                "Está frio: na moto, na bicicleta ou a pé, vale estar bem agasalhado.",
                "FRIO", "WEATHER", List.of(MOTO, BIKE, WALK), List.of(), 0.85, 1.0, false);
        }

        // ---------------- TRANSITO ----------------
        if (traffic == TrafficLevel.VERY_HIGH) {
            add(c, "HEAVY_TRAFFIC", "WARNING", "TRAFFIC", "Trânsito intenso",
                "O trânsito está muito carregado: carro e corridas por aplicativo devem demorar mais.",
                "TRÂNSITO INTENSO", "WARNING", List.of(CAR, RIDE_SHARE), List.of(MOTO, BUS), 0.6, 1.2, false);
        } else if (traffic == TrafficLevel.HIGH) {
            add(c, "MODERATE_TRAFFIC", "INFO", "TRAFFIC", "Trânsito carregado",
                "O trânsito está mais lento que o normal.",
                "TRÂNSITO LENTO", "WARNING", List.of(CAR, RIDE_SHARE), List.of(), 0.85, 1.0, false);
        }
        boolean rush = !weekend && ((hour >= 7 && hour < 9) || (hour >= 17 && hour < 19));
        if (rush && traffic != TrafficLevel.VERY_HIGH) {
            add(c, "RUSH_HOUR", "INFO", "TRAFFIC", "Horário de pico",
                "É horário de pico: o trânsito e o transporte público costumam ficar mais cheios.",
                "HORÁRIO DE PICO", "INFO", List.of(CAR, RIDE_SHARE), List.of(), 0.9, 1.0, false);
        }

        // ---------------- DESTINO ----------------
        if (dest == DestinationType.BAR) {
            add(c, "BAR_SAFETY", "CRITICAL", "SAFETY", "Destino é um bar",
                "Como o destino é um bar, as opções de dirigir foram deixadas de lado por segurança.",
                "LEI SECA", "DANGER", List.of(CAR, MOTO), List.of(RIDE_SHARE, BUS), 0.1, 1.3, true);
        }
        if ((dest == DestinationType.BAR || dest == DestinationType.RESTAURANT)
                && weekend && (tod == TimeOfDay.NIGHT || tod == TimeOfDay.LATE_NIGHT)) {
            add(c, "NIGHTLIFE", "INFO", "SAFETY", "Noite de fim de semana",
                "Em noites de fim de semana, voltar de aplicativo costuma ser mais seguro.",
                "NOITE DE FIM DE SEMANA", "INFO", List.of(WALK, BIKE), List.of(RIDE_SHARE), 0.85, 1.15, false);
        }
        if (dest == DestinationType.AIRPORT) {
            add(c, "AIRPORT_PARKING", "WARNING", "DESTINATION", "Aeroporto",
                "Estacionar no aeroporto costuma ser caro; o aplicativo deixa você na porta.",
                "ESTACIONAMENTO CARO", "WARNING", List.of(CAR), List.of(RIDE_SHARE), 0.5, 1.3, false);
        }
        if (dest == DestinationType.HOSPITAL) {
            add(c, "HOSPITAL_URGENCY", "CRITICAL", "DESTINATION", "Destino hospitalar",
                "Como o destino é um hospital, a prioridade é chegar o mais rápido possível.",
                "MAIS DEMORADO", "WARNING", List.of(BUS, WALK, BIKE), List.of(RIDE_SHARE, CAR), 0.4, 1.4, false);
        }
        if (dest == DestinationType.SHOPPING_MALL) {
            add(c, "MALL_PARKING", "INFO", "DESTINATION", "Shopping",
                "Shoppings costumam cobrar estacionamento.",
                "ESTACIONAMENTO PAGO", "INFO", List.of(CAR), List.of(), 0.85, 1.0, false);
        }
        if ((dest == DestinationType.GYM || dest == DestinationType.PARK) && km <= 5) {
            add(c, "ACTIVE_DESTINATION", "INFO", "DESTINATION", "Destino ativo",
                "O destino combina com ir pedalando ou caminhando, e o trajeto é curto.",
                null, null, List.of(), List.of(BIKE, WALK), 0.0, 1.2, false);
        }
        if (dest == DestinationType.UNIVERSITY && !weekend && tod != TimeOfDay.LATE_NIGHT) {
            add(c, "STUDY_TRIP", "INFO", "DESTINATION", "Faculdade",
                "Para quem vai à faculdade todo dia, o custo da viagem pesa no fim do mês.",
                null, null, List.of(), List.of(BUS, BIKE), 0.0, 1.1, false);
        }

        // ---------------- DISTANCIA ----------------
        if (km <= 1.5) {
            add(c, "SHORT_DISTANCE", "INFO", "DISTANCE", "Trajeto curto",
                "O trajeto é curto: dá para ir caminhando ou de bicicleta sem gastar nada.",
                "TRAJETO CURTO", "INFO", List.of(CAR, RIDE_SHARE), List.of(WALK, BIKE), 0.7, 1.5, false);
        } else if (km > 30) {
            add(c, "LONG_DISTANCE", "INFO", "DISTANCE", "Viagem longa",
                "A distância é grande para ir de bicicleta ou a pé.",
                "DISTÂNCIA LONGA", "WARNING", List.of(BIKE, WALK), List.of(CAR, RIDE_SHARE, BUS), 0.2, 1.2, false);
        }

        // ---------------- HORARIO ----------------
        if (tod == TimeOfDay.NIGHT) {
            add(c, "NIGHT_VISIBILITY", "INFO", "SAFETY", "Período noturno",
                "Já escureceu: de bicicleta ou a pé, prefira vias iluminadas e movimentadas.",
                "TRAJETO NOTURNO", "WARNING", List.of(WALK, BIKE), List.of(), 0.8, 1.0, false);
        }
        if (tod == TimeOfDay.LATE_NIGHT) {
            add(c, "LATE_NIGHT_SAFETY", "WARNING", "SAFETY", "Madrugada",
                "De madrugada, as ruas ficam mais vazias: caminhar ou pedalar é menos seguro.",
                "MADRUGADA", "WARNING", List.of(WALK, BIKE), List.of(RIDE_SHARE, CAR), 0.3, 1.4, false);
            add(c, "LATE_NIGHT_BUS", "INFO", "SAFETY", "Poucos ônibus",
                "De madrugada há poucas linhas de ônibus circulando.",
                "POUCOS ÔNIBUS", "WARNING", List.of(BUS), List.of(), 0.7, 1.0, false);
        }
    }

    private void add(ContextAnalysisResult c, String id, String severity, String category, String title,
                     String message, String tag, String tagType, List<TransportMode> penalized,
                     List<TransportMode> boosted, double penaltyFactor, double boostFactor, boolean hard) {
        c.addInsight(ContextInsight.builder().id(id).severity(severity).category(category)
            .title(title).message(message).tag(tag).tagType(tagType)
            .penalizedModes(penalized).boostedModes(boosted)
            .penaltyFactor(penaltyFactor).boostFactor(boostFactor).hardRestriction(hard).build());
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

}