package com.smartway.api.application.service;

import com.smartway.api.domain.enums.TransportMode;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import com.smartway.api.domain.model.RouteOption;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Converte o ranking do motor no formato de cards usado pelo app. */
@Component
public class RecommendationPresenter {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH'h'mm");
    private static final Locale BR = new Locale("pt", "BR");

    public Map<String, Object> present(List<RouteOption> ranked, ContextAnalysisResult ctx, String bibiMessage) {
        Map<String, Object> card = new LinkedHashMap<>();
        if (ranked == null || ranked.isEmpty()) return card;

        RouteOption best = ranked.get(0);
        Map<String, Object> rec = base(best);
        rec.put("bibiMessage", bibiMessage);
        card.put("recommended", rec);

        List<Map<String, Object>> alternatives = new ArrayList<>();
        for (RouteOption opt : ranked.subList(1, ranked.size())) {
            Map<String, Object> alt = base(opt);
            alt.put("tag", tagFor(opt, best, ctx));
            alternatives.add(alt);
        }
        card.put("alternatives", alternatives);
        return card;
    }

    private Map<String, Object> base(RouteOption o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", o.getMode().name());
        m.put("label", label(o.getMode()));
        m.put("icon", icon(o.getMode()));
        m.put("priceFormatted", o.getEstimatedCost() == 0 ? "Grátis"
            : "R$ " + String.format(BR, "%.2f", o.getEstimatedCost()));
        m.put("durationMinutes", o.getEstimatedTimeMinutes());
        m.put("arrivalTime", LocalTime.now(ZONE).plusMinutes(o.getEstimatedTimeMinutes()).format(HOUR));
        return m;
    }

    // Uma tag por opcao, na ordem de prioridade: seguranca > custo > distancia > vantagens
    private Map<String, String> tagFor(RouteOption opt, RouteOption best, ContextAnalysisResult ctx) {
        Set<String> hits = new HashSet<>();
        if (ctx.getInsights() != null) {
            for (ContextInsight i : ctx.getInsights()) {
                if (i.getPenalizedModes() != null && i.getPenalizedModes().contains(opt.getMode())) {
                    hits.add(i.getId());
                }
            }
        }
        if (hits.contains("RAIN_WARNING") || hits.contains("STORM_BLOCK") || hits.contains("STORM_MOTORCYCLE"))
            return tag("RAIN", "PREVISÃO DE CHUVA", "WEATHER");
        if (hits.contains("NIGHT_VISIBILITY") || hits.contains("LATE_NIGHT_SAFETY") || hits.contains("LATE_NIGHT"))
            return tag("NIGHT", "TRAJETO NOTURNO", "WARNING");
        // Vantagem clara de tempo aparece antes do custo: mostra a troca, nao so o defeito
        if (best.getEstimatedTimeMinutes() - opt.getEstimatedTimeMinutes() >= 10)
            return tag("FASTER", "MAIS RÁPIDO", "POSITIVE");
        if (best.getEstimatedCost() > 0 && opt.getEstimatedCost() > best.getEstimatedCost() * 1.5
            || best.getEstimatedCost() == 0 && opt.getEstimatedCost() > 0)
            return tag("HIGHER_COST", "CUSTO MAIOR", "WARNING");
        String d = opt.getDetails() == null ? "" : opt.getDetails();
        if (d.contains("Distancia longa"))
            return tag("LONG_DISTANCE", "DISTÂNCIA LONGA", "WARNING");
        if (opt.getEstimatedCost() < best.getEstimatedCost())
            return tag("CHEAPER", "MAIS BARATO", "POSITIVE");
        if (opt.getEstimatedTimeMinutes() < best.getEstimatedTimeMinutes())
            return tag("FASTER", "MAIS RÁPIDO", "POSITIVE");
        return null;
    }

    private Map<String, String> tag(String code, String label, String type) {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("code", code); t.put("label", label); t.put("type", type);
        return t;
    }

    private String label(TransportMode m) {
        return switch (m) {
            case CAR -> "Carro próprio";
            case MOTORCYCLE -> "Moto";
            case BUS -> "Ônibus";
            case TRAIN -> "Trem";
            case UBER -> "Carro de aplicativo";
            case NINETY_NINE -> "99";
            case BICYCLE -> "Bicicleta";
            case WALKING -> "A pé";
        };
    }

    private String icon(TransportMode m) {
        return switch (m) {
            case CAR, UBER, NINETY_NINE -> "car";
            case MOTORCYCLE -> "motorcycle";
            case BUS -> "bus";
            case TRAIN -> "train";
            case BICYCLE -> "bike";
            case WALKING -> "walk";
        };
    }
}
