package com.smartway.api.application.service;

import com.smartway.api.adapter.in.dto.request.RecommendationRequest;
import com.smartway.api.domain.enums.TransportMode;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import com.smartway.api.domain.model.RouteOption;
import com.smartway.api.domain.model.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ContextAnalyzerService contextAnalyzer;
    private final ScoringEngineService scoringEngine;
    private final IntelligenceEngineService intelligenceEngine;
    private final RecommendationPresenter presenter;

    public Map<String, Object> recommend(RecommendationRequest request) {
        return recommend(request, null);
    }

    public Map<String, Object> recommend(RecommendationRequest request, com.smartway.api.domain.entity.UserPreference pref) {
        log.info("Processando recomendacao: {} -> {}", request.getOrigin(), request.getDestination());

        // Nivel 1: Coleta de dados e analise de contexto
        ContextAnalysisResult context = contextAnalyzer.analyze(request);

        // Nivel 2: Scoring de cada modal
        List<RouteOption> options = scoringEngine.score(request, context, pref);

        // Nivel 3: Ordenar por score
        options.sort(Comparator.comparingDouble(RouteOption::getScore).reversed());

        RouteOption recommended = options.isEmpty() ? null : options.get(0);

        // Nivel 4: Inteligencia - explicacao humanizada
        String explanation = "";
        if (recommended != null) {
            explanation = intelligenceEngine.explain(recommended, options.size() > 1 ? options.get(1) : null, context, request);
        }

        // Monta resposta
        List<Map<String, Object>> optionsList = new ArrayList<>();
        for (RouteOption opt : options) {
            optionsList.add(Map.of(
                "mode", opt.getMode().name(),
                "score", opt.getScore(),
                "estimatedCost", opt.getEstimatedCost(),
                "estimatedTimeMinutes", opt.getEstimatedTimeMinutes(),
                "details", opt.getDetails() != null ? opt.getDetails() : ""
            ));
        }

        // Monta insights
        List<Map<String, Object>> insightsList = new ArrayList<>();
        if (context.getInsights() != null) {
            for (ContextInsight insight : context.getInsights()) {
                insightsList.add(Map.of(
                    "id", insight.getId() != null ? insight.getId() : "",
                    "severity", insight.getSeverity() != null ? insight.getSeverity() : "INFO",
                    "title", insight.getTitle() != null ? insight.getTitle() : "",
                    "message", insight.getMessage() != null ? insight.getMessage() : ""
                ));
            }
        }

        // Monta contexto resumido
        Map<String, Object> contextSummary = Map.of(
            "weather", context.getWeather() != null ? context.getWeather().name() : "UNKNOWN",
            "temperature", context.getTemperatureCelsius(),
            "traffic", context.getTrafficLevel() != null ? context.getTrafficLevel().name() : "UNKNOWN",
            "destinationType", context.getDestinationType() != null ? context.getDestinationType().name() : "UNKNOWN",
            "timeOfDay", context.getTimeOfDay() != null ? context.getTimeOfDay().name() : "UNKNOWN",
            "intent", context.getIntent() != null ? context.getIntent() : "GENERAL"
        );

        return presenter.present(options, context, explanation);
    }
}