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

    public record Trait(String name, String icon) {}

    public record Profile(String archetype, String displayName, String description, String icon, List<Trait> traits,
                          Map<String, Double> weights, List<TransportMode> availableModes, List<String> priorities, String multimodal, String transitPreference) {}

    public Profile analyze(List<String> priorities, List<String> modes, String multimodal) {
        Set<String> p = normalizePriorities(priorities);
        if (p.size() > 2) throw new IllegalStateException("Escolha ate duas opcoes em 'O que mais importa para voce?'");
        String code = archetypeFor(p);
        Archetype a = archetypeRepository.findByName(code)
            .or(() -> archetypeRepository.findByName("EQUILIBRADO"))
            .orElseThrow(() -> new IllegalStateException("Arquetipo padrao nao cadastrado"));
        return new Profile(code, displayName(code), description(code), icon(code), traits(code),
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

    private static Trait t(String name, String icon) { return new Trait(name, icon); }

    public String description(String code) {
        return switch (code) {
            case "APRESSADO" -> "Você gosta de chegar ao destino o mais rápido possível e prefere rotas que ajudam a economizar tempo no dia a dia.";
            case "ECONOMICO" -> "Você procura maneiras de gastar menos nos seus deslocamentos e prefere rotas que ajudam a economizar dinheiro.";
            case "CUIDADOSO" -> "Você valoriza conforto e segurança durante seus deslocamentos e prefere trajetos mais simples e tranquilos.";
            case "PRATICO" -> "Você gosta de deslocamentos simples e sem complicação, com menos trocas e menos esforço no caminho.";
            case "SUSTENTAVEL" -> "Você se preocupa com o meio ambiente e prefere meios de transporte que poluem menos e mantêm você em movimento.";
            case "SABIO" -> "Você sabe equilibrar tempo e dinheiro, escolhendo a opção que vale mais a pena em cada trajeto.";
            case "EXECUTIVO" -> "Você precisa chegar no horário e com tranquilidade, preferindo rotas rápidas, seguras e sem imprevistos.";
            case "DINAMICO" -> "Você gosta de agilidade sem deixar o meio ambiente de lado, combinando rapidez com escolhas mais sustentáveis.";
            case "PRUDENTE" -> "Você busca economizar sem abrir mão da segurança, escolhendo trajetos confiáveis e com bom custo.";
            case "CONSCIENTE" -> "Você pensa no seu bolso e no planeta, preferindo opções econômicas e que causam menos impacto ambiental.";
            default -> "Você busca um equilíbrio entre tempo, custo e conforto, escolhendo rotas que combinam com sua rotina.";
        };
    }

    public String icon(String code) {
        return switch (code) {
            case "APRESSADO" -> "flash-outline";
            case "ECONOMICO" -> "wallet-outline";
            case "CUIDADOSO" -> "shield-checkmark-outline";
            case "PRATICO" -> "navigate-outline";
            case "SUSTENTAVEL" -> "leaf-outline";
            case "SABIO" -> "bulb-outline";
            case "EXECUTIVO" -> "briefcase-outline";
            case "DINAMICO" -> "bicycle-outline";
            case "PRUDENTE" -> "lock-closed-outline";
            case "CONSCIENTE" -> "earth-outline";
            default -> "git-compare-outline";
        };
    }

    public List<Trait> traits(String code) {
        return switch (code) {
            case "APRESSADO" -> List.of(t("Rápido", "flash-outline"), t("Objetivo", "locate-outline"), t("Dinâmico", "speedometer-outline"));
            case "ECONOMICO" -> List.of(t("Econômico", "wallet-outline"), t("Consciente", "leaf-outline"), t("Planejado", "calendar-outline"));
            case "CUIDADOSO" -> List.of(t("Confortável", "heart-outline"), t("Seguro", "shield-checkmark-outline"), t("Tranquilo", "happy-outline"));
            case "PRATICO" -> List.of(t("Simples", "checkmark-circle-outline"), t("Direto", "navigate-outline"), t("Confortável", "heart-outline"));
            case "SUSTENTAVEL" -> List.of(t("Ecológico", "leaf-outline"), t("Ativo", "walk-outline"), t("Consciente", "earth-outline"));
            case "SABIO" -> List.of(t("Rápido", "flash-outline"), t("Econômico", "wallet-outline"), t("Estratégico", "bulb-outline"));
            case "EXECUTIVO" -> List.of(t("Rápido", "flash-outline"), t("Seguro", "shield-checkmark-outline"), t("Pontual", "time-outline"));
            case "DINAMICO" -> List.of(t("Rápido", "flash-outline"), t("Ecológico", "leaf-outline"), t("Ativo", "bicycle-outline"));
            case "PRUDENTE" -> List.of(t("Econômico", "wallet-outline"), t("Seguro", "shield-checkmark-outline"), t("Planejado", "calendar-outline"));
            case "CONSCIENTE" -> List.of(t("Econômico", "wallet-outline"), t("Ecológico", "leaf-outline"), t("Responsável", "earth-outline"));
            default -> List.of(t("Prático", "navigate-outline"), t("Econômico", "wallet-outline"), t("Confortável", "shield-checkmark-outline"));
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
