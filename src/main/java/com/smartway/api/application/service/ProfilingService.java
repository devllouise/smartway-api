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
                          Map<String, Double> weights, List<TransportMode> availableModes, List<String> priorities, String multimodal, String transitPreference) {}

    public Profile analyze(List<String> priorities, List<String> modes, String multimodal) {
        Set<String> p = normalizePriorities(priorities);
        if (p.size() > 2) throw new IllegalStateException("Escolha ate duas opcoes em 'O que mais importa para voce?'");
        String code = archetypeFor(p);
        Archetype a = archetypeRepository.findByName(code)
            .or(() -> archetypeRepository.findByName("EQUILIBRADO"))
            .orElseThrow(() -> new IllegalStateException("Arquetipo padrao nao cadastrado"));
        return new Profile(code, displayName(code), a.getDescription(), traits(code),
            weights(a, p), availableModes(modes), new ArrayList<>(p),
            pick(multimodal, MULTI, "IF_WORTH"),
            normalize(modes).contains("PUBLIC_TRANSPORT") ? deduceTransit(p, pick(multimodal, MULTI, "IF_WORTH")) : null);
    }

    private Set<String> normalize(List<String> list) {
        Set<String> s = new LinkedHashSet<>();
        if (list != null) for (String v : list) if (v != null && !v.isBlank()) s.add(v.trim().toUpperCase());
        return s;
    }

    // Codigos desconhecidos sao ignorados
    private static final Set<String> PRIORITIES = Set.of("TIME", "COST", "SAFETY", "PRACTICAL", "ECO");

    private Set<String> normalizePriorities(List<String> list) {
        Set<String> out = new LinkedHashSet<>();
        for (String v : normalize(list)) {
            if (PRIORITIES.contains(v)) out.add(v);
        }
        return out;
    }

    // Uma prioridade ou um par -> arquetipo (ordem do par nao importa)
    private static final Map<Set<String>, String> ARCHETYPES = Map.ofEntries(
        Map.entry(Set.of("TIME"), "APRESSADO"),
        Map.entry(Set.of("COST"), "ECONOMICO"),
        Map.entry(Set.of("SAFETY"), "CUIDADOSO"),
        Map.entry(Set.of("PRACTICAL"), "PRATICO"),
        Map.entry(Set.of("ECO"), "SUSTENTAVEL"),
        Map.entry(Set.of("TIME", "COST"), "SABIO"),
        Map.entry(Set.of("TIME", "SAFETY"), "EXECUTIVO"),
        Map.entry(Set.of("TIME", "PRACTICAL"), "EXECUTIVO"),
        Map.entry(Set.of("TIME", "ECO"), "DINAMICO"),
        Map.entry(Set.of("COST", "SAFETY"), "PRUDENTE"),
        Map.entry(Set.of("COST", "PRACTICAL"), "PRATICO"),
        Map.entry(Set.of("COST", "ECO"), "CONSCIENTE"),
        Map.entry(Set.of("SAFETY", "PRACTICAL"), "CUIDADOSO"),
        Map.entry(Set.of("SAFETY", "ECO"), "CONSCIENTE"),
        Map.entry(Set.of("PRACTICAL", "ECO"), "SUSTENTAVEL"));

    private String archetypeFor(Set<String> p) {
        return ARCHETYPES.getOrDefault(p, "EQUILIBRADO");
    }

    // Peso do arquetipo x (1.0 se marcado, 0.25 se nao), normalizado para somar 1
    private Map<String, Double> weights(Archetype a, Set<String> p) {
        boolean none = p.isEmpty();
        double cost = a.getDefaultCostWeight() * f(none || p.contains("COST"));
        double time = a.getDefaultTimeWeight() * f(none || p.contains("TIME"));
        double comfort = a.getDefaultComfortWeight() * f(none || p.contains("SAFETY") || p.contains("PRACTICAL"));
        double safety = a.getDefaultSafetyWeight() * f(none || p.contains("SAFETY"));
        double eco = a.getDefaultEcoWeight() * f(none || p.contains("ECO"));
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
                case "RIDE_SHARE" -> out.add(TransportMode.RIDE_SHARE);
                case "MOTORCYCLE" -> out.add(TransportMode.MOTORCYCLE);
                case "BICYCLE" -> out.add(TransportMode.BICYCLE);
                case "WALKING" -> out.add(TransportMode.WALKING);
                default -> { }
            }
        }
        if (out.isEmpty()) out.addAll(List.of(TransportMode.BUS, TransportMode.RIDE_SHARE,
            TransportMode.BICYCLE, TransportMode.WALKING));
        return new ArrayList<>(out);
    }

    public String displayName(String code) {
        return switch (code) {
            case "SABIO" -> "Sábio";
            case "APRESSADO" -> "Ágil";
            case "ECONOMICO" -> "Econômico";
            case "SUSTENTAVEL" -> "Sustentável";
            case "CUIDADOSO" -> "Cuidadoso";
            case "EXECUTIVO" -> "Executivo";
            case "PRUDENTE" -> "Prudente";
            case "PRATICO" -> "Prático";
            case "CONSCIENTE" -> "Consciente";
            case "DINAMICO" -> "Dinâmico";
            default -> "Equilibrado";
        };
    }

    public List<String> traits(String code) {
        return switch (code) {
            case "SABIO" -> List.of("Rápido", "Econômico", "Estratégico");
            case "APRESSADO" -> List.of("Rápido", "Prático", "Pontual");
            case "ECONOMICO" -> List.of("Econômico", "Planejado", "Consciente");
            case "SUSTENTAVEL" -> List.of("Ecológico", "Ativo", "Consciente");
            case "CUIDADOSO" -> List.of("Seguro", "Confortável", "Tranquilo");
            case "EXECUTIVO" -> List.of("Rápido", "Seguro", "Pontual");
            case "PRUDENTE" -> List.of("Econômico", "Seguro", "Planejado");
            case "PRATICO" -> List.of("Simples", "Direto", "Confortável");
            case "CONSCIENTE" -> List.of("Econômico", "Ecológico", "Responsável");
            case "DINAMICO" -> List.of("Rápido", "Ecológico", "Ativo");
            default -> List.of("Versátil", "Equilibrado", "Adaptável");
        };
    }

    private static final Set<String> MULTI = Set.of("ANY", "IF_WORTH", "SINGLE", "DEPENDS");

    // A preferencia no transporte publico nao e perguntada: e deduzida das perguntas 1 e 3
    private String deduceTransit(Set<String> p, String multi) {
        if ("SINGLE".equals(multi) || "DEPENDS".equals(multi)) return "FEWER_TRANSFERS";
        if (p.contains("SAFETY") || p.contains("PRACTICAL")) return "COMFORT";
        if (p.contains("COST") && !p.contains("TIME")) return "CHEAPER";
        return "FASTER";
    }

    private String pick(String v, Set<String> allowed, String def) {
        if (v == null || v.isBlank()) return def;
        String s = v.trim().toUpperCase();
        return allowed.contains(s) ? s : def;
    }
}
