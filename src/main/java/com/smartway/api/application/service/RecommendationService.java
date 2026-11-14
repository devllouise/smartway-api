package com.smartway.api.application.service;

import com.smartway.api.adapter.in.dto.request.RecommendationRequest;
import com.smartway.api.domain.entity.UserPreference;
import com.smartway.api.domain.enums.TransportMode;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.RouteOption;
import com.smartway.api.port.out.TransitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final TransitPort transitPort;
    private final LearningService learningService;

    public record Result(Map<String, Object> card, ContextAnalysisResult context) {}

    public Result recommend(RecommendationRequest request, UserPreference pref, Long userId) {
        log.info("Processando recomendacao: {} -> {}", request.getOrigin(), request.getDestination());

        // Nivel 1: contexto (rota, clima, tipo de destino, horario) + transporte publico real
        ContextAnalysisResult context = contextAnalyzer.analyze(request);
        if (usesPublicTransport(pref)) {
            context.setTransit(transitPort.findRoute(request.getOrigin(), request.getDestination(),
                pref != null ? pref.getTransitPreference() : null).orElse(null));
        }

        // Nivel 4: habito aprendido nesse contexto (reforco leve no scoring)
        TransportMode habit = learningService.habitualMode(userId,
            context.getDestinationType() != null ? context.getDestinationType().name() : null,
            context.getTimeOfDay() != null ? context.getTimeOfDay().name() : null).orElse(null);

        // Nivel 2: scoring multicriterio com os pesos do perfil (ja ordenado)
        List<RouteOption> options = scoringEngine.score(request, context, pref, habit);
        RouteOption recommended = options.isEmpty() ? null : options.get(0);

        // Nivel 3: a Bibi explica a decisao (nao decide)
        String explanation = recommended == null ? ""
            : intelligenceEngine.explain(recommended, options.size() > 1 ? options.get(1) : null, context, request);

        return new Result(presenter.present(options, context, explanation), context);
    }

    private boolean usesPublicTransport(UserPreference p) {
        return p == null || p.getAvailableModes() == null || p.getAvailableModes().isBlank()
            || p.getAvailableModes().contains("BUS");
    }
}
