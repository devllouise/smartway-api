package com.smartway.api.application.service;

import com.smartway.api.adapter.in.dto.request.RecommendationRequest;
import com.smartway.api.domain.entity.UserPreference;
import com.smartway.api.domain.enums.TransportMode;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import com.smartway.api.domain.model.RouteOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@lombok.RequiredArgsConstructor
public class ScoringEngineService {

    private final VehicleService vehicleService;

    private static final List<TransportMode> DEFAULT_MODES = List.of(
        TransportMode.CAR, TransportMode.MOTORCYCLE, TransportMode.BUS,
        TransportMode.RIDE_SHARE, TransportMode.BICYCLE, TransportMode.WALKING);

    private static final double MAX_WALK_KM = 3.0;
    private static final double MAX_BIKE_KM = 12.0;
    private static final double DEFAULT_PENALTY = 0.6;
    private static final double DEFAULT_BOOST = 1.3;

    /** Pesos do perfil do usuario. Sem perfil, usa o equilibrado. */
    record Weights(double cost, double time, double comfort, double safety, double eco) {
        static Weights from(UserPreference p) {
            if (p == null) return new Weights(0.30, 0.30, 0.20, 0.10, 0.10);
            return new Weights(p.getCostWeight(), p.getTimeWeight(), p.getComfortWeight(),
                p.getSafetyWeight(), p.getEcoWeight());
        }
    }

    public List<RouteOption> score(RecommendationRequest request, ContextAnalysisResult context,
                                   UserPreference pref, TransportMode habit) {
        double distance = context.getDistanceKm();
        Weights w = Weights.from(pref);
        List<TransportMode> modes = candidateModes(request, pref);

        List<RouteOption> options = new ArrayList<>();
        Long userId = pref != null && pref.getUser() != null ? pref.getUser().getId() : null;
        double toll = context.getTollCost();
        for (TransportMode mode : modes) options.add(buildOption(mode, distance, w, userId, toll));
        applyTransit(options, context.getTransit(), w, pref);

        for (RouteOption opt : options) {
            if (opt.getMode() == TransportMode.WALKING && distance > MAX_WALK_KM) penalize(opt, 0.3, "Distancia longa para caminhar");
            if (opt.getMode() == TransportMode.BICYCLE && distance > MAX_BIKE_KM) penalize(opt, 0.4, "Distancia longa para bicicleta");
        }
        if (context.getInsights() != null) for (ContextInsight i : context.getInsights()) applyInsight(options, i);
        applyHabit(options, habit);
        applyPriority(options, request.getPriority());

        options.sort(Comparator.comparingDouble(RouteOption::getScore).reversed()
            .thenComparingInt(RouteOption::getEstimatedTimeMinutes));
        log.info("Scoring completo: {} opcoes, pesos={}, modais={}, habito={}", options.size(), w, modes, habit);
        return options;
    }

    // Modais do perfil (triagem). Se a busca trouxer preferredModes, restringe mais.
    private List<TransportMode> candidateModes(RecommendationRequest req, UserPreference pref) {
        List<TransportMode> base = parseModes(pref == null ? null : pref.getAvailableModes());
        if (base.isEmpty()) base = DEFAULT_MODES;
        List<TransportMode> asked = req.getPreferredModes();
        if (asked == null || asked.isEmpty()) return base;
        List<TransportMode> both = new ArrayList<>();
        for (TransportMode m : new LinkedHashSet<>(asked)) if (base.contains(m)) both.add(m);
        return both.isEmpty() ? new ArrayList<>(new LinkedHashSet<>(asked)) : both;
    }

    private List<TransportMode> parseModes(String csv) {
        List<TransportMode> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String s : csv.split(",")) {
            try { out.add(TransportMode.valueOf(s.trim())); } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }

    private RouteOption buildOption(TransportMode mode, double distance, Weights w, Long userId, double toll) {
        double cost = calculateCost(mode, distance, userId) + tollFor(mode, toll);
        int time = calculateTime(mode, distance);
        return RouteOption.builder().mode(mode).score(baseScore(mode, cost, time, w))
            .estimatedCost(round(cost)).estimatedTimeMinutes(time).distanceKm(distance).details("").build();
    }

    // Cada criterio vira nota de 0 a 10; o score e a media ponderada pelos pesos do perfil
    private double baseScore(TransportMode m, double cost, int time, Weights w) {
        double costScore = 10.0 / (1.0 + cost / 5.0);
        double timeScore = 10.0 / (1.0 + time / 20.0);
        double sum = w.cost() + w.time() + w.comfort() + w.safety() + w.eco();
        if (sum <= 0) sum = 1;
        return (w.cost() * costScore + w.time() * timeScore + w.comfort() * comfort(m)
            + w.safety() * safety(m) + w.eco() * eco(m)) / sum;
    }

    private double comfort(TransportMode m) {
        return switch (m) { case CAR, RIDE_SHARE -> 9; case TRAIN -> 6; case BUS -> 5;
            case MOTORCYCLE -> 4; case BICYCLE, WALKING -> 3; };
    }

    private double safety(TransportMode m) {
        return switch (m) { case CAR, RIDE_SHARE, TRAIN -> 8; case BUS -> 7; case WALKING -> 5;
            case BICYCLE -> 4; case MOTORCYCLE -> 3; };
    }

    private double eco(TransportMode m) {
        return switch (m) { case WALKING, BICYCLE -> 10; case TRAIN -> 8; case BUS -> 7;
            case MOTORCYCLE -> 4; case CAR, RIDE_SHARE -> 2; };
    }

    // Carro e moto: combustivel pelo consumo do veiculo do usuario (Inmetro/km/l informado) x preco do litro
    private double calculateCost(TransportMode mode, double d, Long userId) {
        return switch (mode) {
            case CAR -> vehicleService.fuelCost(userId, "CAR", d);
            case MOTORCYCLE -> vehicleService.fuelCost(userId, "MOTORCYCLE", d);
            case BUS, TRAIN -> 4.40;
            case RIDE_SHARE -> 7.0 + d * 1.80;
            case BICYCLE, WALKING -> 0.0;
        };
    }

    // Pedagio real da rota (Google), so para quem dirige. Moto paga metade da tarifa de carro
    // nas rodovias paulistas. Carro de aplicativo nao soma: o valor ja vem na estimativa da corrida.
    private double tollFor(TransportMode mode, double toll) {
        if (toll <= 0) return 0;
        return switch (mode) {
            case CAR -> toll;
            case MOTORCYCLE -> toll * 0.5;
            default -> 0;
        };
    }

    private int calculateTime(TransportMode mode, double d) {
        return switch (mode) {
            case CAR -> (int) Math.ceil(d / 40.0 * 60);
            case MOTORCYCLE -> (int) Math.ceil(d / 35.0 * 60);
            case BUS -> (int) Math.ceil(d / 20.0 * 60 + 15);
            case TRAIN -> (int) Math.ceil(d / 25.0 * 60 + 10);
            case RIDE_SHARE -> (int) Math.ceil(d / 40.0 * 60 + 5);
            case BICYCLE -> (int) Math.ceil(d / 15.0 * 60);
            case WALKING -> (int) Math.ceil(d / 5.0 * 60);
        };
    }

    private void applyInsight(List<RouteOption> options, ContextInsight insight) {
        double penalty = validFactor(insight.getPenaltyFactor(), DEFAULT_PENALTY, true);
        double boost = validFactor(insight.getBoostFactor(), DEFAULT_BOOST, false);
        for (RouteOption opt : options) {
            if (insight.getPenalizedModes() != null && hits(insight.getPenalizedModes(), opt.getMode())) {
                if (insight.isHardRestriction()) {
                    opt.setScore(0.01);
                    opt.setDetails("BLOQUEADO: " + insight.getTitle());
                } else {
                    penalize(opt, penalty, insight.getTitle());
                }
            }
            if (insight.getBoostedModes() != null && hits(insight.getBoostedModes(), opt.getMode())) {
                opt.setScore(opt.getScore() * boost);
            }
        }
    }

    private double validFactor(double f, double def, boolean isPenalty) {
        if (isPenalty) return (f > 0 && f < 1) ? f : def;
        return (f > 1) ? f : def;
    }

    // Ajuste opcional so para esta busca ("hoje estou com pressa")
    private void applyPriority(List<RouteOption> options, String priority) {
        if (priority == null) return;
        for (RouteOption opt : options) {
            switch (priority.toUpperCase()) {
                case "COST" -> opt.setScore(opt.getScore() * (1.0 + 1.0 / (1.0 + opt.getEstimatedCost() / 5.0)));
                case "TIME" -> opt.setScore(opt.getScore() * (1.0 + 1.0 / (1.0 + opt.getEstimatedTimeMinutes() / 15.0)));
                case "COMFORT" -> {
                    if (opt.getMode() == TransportMode.CAR || opt.getMode() == TransportMode.RIDE_SHARE) opt.setScore(opt.getScore() * 1.3);
                }
                default -> { }
            }
        }
    }

    private void penalize(RouteOption opt, double factor, String reason) {
        opt.setScore(opt.getScore() * factor);
        String d = opt.getDetails();
        opt.setDetails((d == null || d.isBlank()) ? reason : d + "; " + reason);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // Modal afetado pela regra
    private boolean hits(List<TransportMode> list, TransportMode m) {
        return list.contains(m);
    }

    // Onibus com dados reais; a preferencia multimodal da triagem decide se a combinacao vale
    private void applyTransit(List<RouteOption> options, com.smartway.api.domain.model.TransitRoute t,
                              Weights w, UserPreference pref) {
        if (t == null) return;
        for (RouteOption opt : options) {
            if (opt.getMode() != TransportMode.BUS) continue;
            double cost = t.fareKnown() ? t.fare() : opt.getEstimatedCost();
            opt.setEstimatedCost(round(cost));
            opt.setEstimatedTimeMinutes(t.minutes());
            opt.setLegs(t.legs());
            opt.setScore(baseScore(TransportMode.BUS, cost, t.minutes(), w));
            int walk = t.walkMinutes();
            if (walk >= 5) opt.setLabel("Ônibus e caminhada");

            String multi = pref == null || pref.getMultimodal() == null ? "IF_WORTH" : pref.getMultimodal();
            if ("SINGLE".equals(multi) && walk >= 5) penalize(opt, 0.6, "Prefere um unico meio");
            if ("DEPENDS".equals(multi) && t.busLegs() > 1) penalize(opt, 0.7, "Mais de uma baldeacao");
            if (!"ANY".equals(multi) && walk > 20) penalize(opt, 0.8, "Caminhada longa ate o ponto");
        }
    }

    // Nivel 4: o modal que a pessoa costuma escolher nesse contexto ganha um reforco leve
    // (nunca vence uma restricao de seguranca)
    private void applyHabit(List<RouteOption> options, TransportMode habit) {
        if (habit == null) return;
        for (RouteOption opt : options) {
            if (opt.getMode() == habit && opt.getScore() > 0.01) {
                opt.setScore(opt.getScore() * 1.15);
                String d = opt.getDetails();
                opt.setDetails((d == null || d.isBlank()) ? "Seu costume neste trajeto" : d + "; Seu costume neste trajeto");
            }
        }
    }
}
