package com.smartway.api.application.service;

import com.smartway.api.adapter.out.persistence.ArchetypeRepository;
import com.smartway.api.domain.entity.Archetype;
import com.smartway.api.domain.enums.TransportMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/** Converte as respostas da triagem em arquetipo, pesos e modais disponiveis. */
@Service
@RequiredArgsConstructor
public class ProfilingService {

    private final ArchetypeRepository archetypeRepository;

    public record Profile(String archetype, String displayName, String description, List<String> traits,
                          Map<String, Double> weights, List<TransportMode> availableModes, List<String> priorities) {}

    public Profile analyze(List<String> priorities, List<String> modes) {
        Set<String> p = normalize(priorities);
        String code = archetypeFor(p);
        Archetype a = archetypeRepository.findByName(code)
            .or(() -> archetypeRepository.findByName("EQUILIBRADO"))
            .orElseThrow(() -> new IllegalStateException("Arquetipo padrao nao cadastrado"));
        return new Profile(code, displayName(code), a.getDescription(), traits(code),
            weights(a, p), availableModes(modes), new ArrayList<>(p));
    }

    private Set<String> normalize(List<String> list) {
        Set<String> s = new LinkedHashSet<>();
        if (list != null) for (String v : list) if (v != null && !v.isBlank()) s.add(v.trim().toUpperCase());
        return s;
    }

    private String archetypeFor(Set<String> p) {
        if (p.isEmpty() || p.size() >= 3) return "EQUILIBRADO";
        if (p.equals(Set.of("TIME", "COST"))) return "SABIO";
        if (p.size() == 2) return "EQUILIBRADO";
        return switch (p.iterator().next()) {
            case "TIME" -> "APRESSADO";
            case "COST" -> "ECONOMICO";
            case "ECO" -> "SUSTENTAVEL";
            case "ACTIVE" -> "ATIVO";
            case "SAFETY" -> "CUIDADOSO";
            default -> "EQUILIBRADO";
        };
    }

    // Peso do arquetipo x (1.0 se marcado, 0.25 se nao), normalizado para somar 1
    private Map<String, Double> weights(Archetype a, Set<String> p) {
        boolean none = p.isEmpty();
        double cost = a.getDefaultCostWeight() * f(none || p.contains("COST"));
        double time = a.getDefaultTimeWeight() * f(none || p.contains("TIME"));
        double comfort = a.getDefaultComfortWeight() * f(none || p.contains("SAFETY"));
        double safety = a.getDefaultSafetyWeight() * f(none || p.contains("SAFETY"));
        double eco = a.getDefaultEcoWeight() * f(none || p.contains("ECO") || p.contains("ACTIVE"));
        double sum = cost + time + comfort + safety + eco;
        Map<String, Double> w = new LinkedHashMap<>();
        w.put("cost", round(cost / sum)); w.put("time", round(time / sum));
        w.put("comfort", round(comfort / sum)); w.put("safety", round(safety / sum));
        w.put("eco", round(eco / sum));
        return w;
    }

    private double f(boolean selected) { return selected ? 1.0 : 0.25; }
    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    // Tela 2: o que a pessoa usa vira filtro permanente de modais
    public List<TransportMode> availableModes(List<String> modes) {
        Set<TransportMode> out = new LinkedHashSet<>();
        for (String m : normalize(modes)) {
            switch (m) {
                case "PUBLIC_TRANSPORT" -> out.add(TransportMode.BUS);
                case "OWN_CAR" -> out.add(TransportMode.CAR);
                case "RIDE_SHARE" -> { out.add(TransportMode.UBER); out.add(TransportMode.NINETY_NINE); }
                case "MOTORCYCLE" -> out.add(TransportMode.MOTORCYCLE);
                case "BICYCLE" -> out.add(TransportMode.BICYCLE);
                case "WALKING" -> out.add(TransportMode.WALKING);
                default -> { }
            }
        }
        if (out.isEmpty()) out.addAll(List.of(TransportMode.BUS, TransportMode.UBER, TransportMode.NINETY_NINE,
            TransportMode.BICYCLE, TransportMode.WALKING));
        return new ArrayList<>(out);
    }

    public String displayName(String code) {
        return switch (code) {
            case "SABIO" -> "Sábio";
            case "APRESSADO" -> "Ágil";
            case "ECONOMICO" -> "Econômico";
            case "SUSTENTAVEL" -> "Sustentável";
            case "ATIVO" -> "Ativo";
            case "CUIDADOSO" -> "Cuidadoso";
            default -> "Equilibrado";
        };
    }

    public List<String> traits(String code) {
        return switch (code) {
            case "SABIO" -> List.of("Rápido", "Econômico");
            case "APRESSADO" -> List.of("Rápido", "Prático");
            case "ECONOMICO" -> List.of("Econômico", "Planejado");
            case "SUSTENTAVEL" -> List.of("Ecológico", "Consciente");
            case "ATIVO" -> List.of("Ativo", "Saudável");
            case "CUIDADOSO" -> List.of("Seguro", "Confortável");
            default -> List.of("Versátil", "Equilibrado");
        };
    }
}
