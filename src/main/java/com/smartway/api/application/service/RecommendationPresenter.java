package com.smartway.api.application.service;

import com.smartway.api.domain.enums.TransportMode;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import com.smartway.api.domain.model.RouteOption;
import com.smartway.api.domain.model.TransitRoute;
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
        m.put("label", o.getLabel() != null ? o.getLabel() : label(o.getMode()));
        m.put("icon", icon(o.getMode()));
        m.put("priceFormatted", price(o));
        m.put("durationMinutes", o.getEstimatedTimeMinutes());
        if (o.getLegs() != null && !o.getLegs().isEmpty()) m.put("legs", o.getLegs());
        m.put("arrivalTime", LocalTime.now(ZONE).plusMinutes(o.getEstimatedTimeMinutes()).format(HOUR));
        return m;
    }

    // Carro de aplicativo: o preco real varia (tarifa dinamica), entao mostra uma faixa, como o Google faz
    private String price(RouteOption o) {
        double v = o.getEstimatedCost();
        if (v == 0) return "Sem custo";
        if (o.getMode() == TransportMode.RIDE_SHARE) {
            long min = Math.max(1, Math.round(Math.floor(v * 0.9)));
            long max = Math.round(Math.ceil(v * 1.15));
            if (max <= min) max = min + 1;
            return "R$ " + min + "–" + max;
        }
        return "R$ " + String.format(BR, "%.2f", v);
    }

    // Uma tag por opcao: 1o o risco mais grave do contexto, depois o trajeto de onibus, depois a troca (tempo x custo)
    private Map<String, String> tagFor(RouteOption opt, RouteOption best, ContextAnalysisResult ctx) {
        ContextInsight worst = null;
        if (ctx.getInsights() != null) {
            for (ContextInsight i : ctx.getInsights()) {
                if (i.getTag() == null || i.getPenalizedModes() == null) continue;
                boolean hit = i.getPenalizedModes().contains(opt.getMode());
                if (hit && (worst == null || rank(i) > rank(worst))) worst = i;
            }
        }
        if (worst != null) return tag(worst.getId(), worst.getTag(), worst.getTagType());

        if (opt.getMode() == TransportMode.BUS && opt.getLegs() != null && !opt.getLegs().isEmpty()) {
            long buses = opt.getLegs().stream().filter(l -> "BUS".equals(l.type())).count();
            if (buses >= 2) return tag("TRANSFER", "COM BALDEAÇÃO", "WARNING");
            int maxWalk = opt.getLegs().stream().filter(l -> "WALK".equals(l.type()))
                .mapToInt(TransitRoute.Leg::minutes).max().orElse(0);
            if (maxWalk >= 8) return tag("LONG_WALK", "TRECHO A PÉ", "WARNING");
        }
        if (best.getEstimatedTimeMinutes() - opt.getEstimatedTimeMinutes() >= 10)
            return tag("FASTER", "MAIS RÁPIDO", "POSITIVE");
        if (opt.getEstimatedCost() - best.getEstimatedCost() >= 2)
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

    // Bloqueio > critico > aviso > informativo
    private int rank(ContextInsight i) {
        int r = switch (i.getSeverity() == null ? "" : i.getSeverity()) {
            case "CRITICAL" -> 3; case "WARNING" -> 2; case "INFO" -> 1; default -> 0; };
        return i.isHardRestriction() ? r + 10 : r;
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
            case RIDE_SHARE -> "Carro de aplicativo";
            case BICYCLE -> "Bicicleta";
            case WALKING -> "A pé";
        };
    }

    private String icon(TransportMode m) {
        return switch (m) {
            case CAR, RIDE_SHARE -> "car";
            case MOTORCYCLE -> "motorcycle";
            case BUS -> "bus";
            case TRAIN -> "train";
            case BICYCLE -> "bike";
            case WALKING -> "walk";
        };
    }
}
