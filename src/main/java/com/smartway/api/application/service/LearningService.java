package com.smartway.api.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartway.api.adapter.out.persistence.*;
import com.smartway.api.application.exception.ResourceNotFoundException;
import com.smartway.api.domain.entity.*;
import com.smartway.api.domain.enums.TransportMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Nivel 4 - aprendizado adaptativo.
 * 1) Habito: o modal que o usuario mais escolhe em um contexto ganha um pequeno reforco no scoring.
 * 2) Pesos: se as escolhas mostram um padrao claro (mais barato ou mais rapido que o recomendado),
 *    o sistema SUGERE um ajuste. Nada muda sem o usuario aceitar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningService {

    private final RouteFeedbackRepository feedbackRepository;
    private final RouteHistoryRepository historyRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final PreferenceHistoryRepository preferenceHistoryRepository;
    private final WeightSuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${smartway.learning.min-interactions:5}")
    private int minInteractions;

    @Value("${smartway.learning.weight-adjustment-rate:0.10}")
    private double adjustmentRate;

    @Value("${smartway.learning.habit-min-occurrences:3}")
    private long habitMinOccurrences;

    public Optional<TransportMode> habitualMode(Long userId, String destinationType, String timeOfDay) {
        if (userId == null || destinationType == null || timeOfDay == null) return Optional.empty();
        try {
            List<String> r = feedbackRepository.habitualMode(userId, destinationType, timeOfDay, habitMinOccurrences);
            return r.isEmpty() ? Optional.empty() : Optional.of(TransportMode.valueOf(r.get(0)));
        } catch (Exception e) {
            log.warn("Nao foi possivel consultar o habito do usuario: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Analisa os feedbacks recentes e cria uma sugestao se houver um padrao claro. */
    @Transactional
    public void evaluate(Long userId) {
        if (suggestionRepository.existsByUserIdAndStatus(userId, "PENDING")) return;
        List<RouteFeedback> recent = feedbackRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        if (recent.size() < minInteractions) return;

        int cheaper = 0, faster = 0, mismatches = 0;
        for (RouteFeedback f : recent) {
            if (f.getChosenMode() == null) continue;
            RouteHistory h = historyRepository.findById(f.getHistoryId()).orElse(null);
            if (h == null || f.getChosenMode().equals(h.getRecommendedMode())) continue;
            mismatches++;
            Map<String, Object> chosen = findOption(h.getCardJson(), f.getChosenMode());
            if (chosen == null) continue;
            double chosenPrice = price(chosen.get("priceFormatted"));
            double recPrice = price(h.getPriceFormatted());
            int chosenTime = chosen.get("durationMinutes") instanceof Number n ? n.intValue() : 0;
            int recTime = h.getDurationMinutes() == null ? 0 : h.getDurationMinutes();
            if (chosenPrice < recPrice && chosenTime >= recTime) cheaper++;
            else if (chosenTime < recTime && chosenPrice >= recPrice) faster++;
        }

        int votes = Math.max(cheaper, faster);
        if (mismatches < 3 || votes < 3 || votes < mismatches * 0.6) return;
        String focus = cheaper >= faster ? "COST" : "TIME";

        UserPreference p = preferenceRepository.findByUserId(userId).orElse(null);
        if (p == null) return;
        double[] w = { p.getCostWeight(), p.getTimeWeight(), p.getComfortWeight(), p.getSafetyWeight(), p.getEcoWeight() };
        w["COST".equals(focus) ? 0 : 1] += adjustmentRate;
        double sum = Arrays.stream(w).sum();
        for (int i = 0; i < w.length; i++) w[i] = Math.round(w[i] / sum * 100.0) / 100.0;

        String reason = "Em " + votes + " de " + mismatches + " viagens voce escolheu uma opcao mais "
            + ("COST".equals(focus) ? "barata" : "rapida") + " que a recomendada.";
        suggestionRepository.save(WeightSuggestion.builder().userId(userId).focus(focus)
            .costWeight(w[0]).timeWeight(w[1]).comfortWeight(w[2]).safetyWeight(w[3]).ecoWeight(w[4])
            .reason(reason).status("PENDING").build());
        log.info("Sugestao de ajuste criada para usuario {}: foco={} ({})", userId, focus, reason);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> pending(Long userId) {
        return suggestionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING").map(this::toMap);
    }

    @Transactional
    public Map<String, Object> accept(Long userId, Long id) {
        WeightSuggestion s = find(userId, id);
        UserPreference p = preferenceRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Preferencias nao encontradas"));

        preferenceHistoryRepository.save(PreferenceHistory.builder()
            .user(userRepository.getReferenceById(userId))
            .costWeight(p.getCostWeight()).timeWeight(p.getTimeWeight())
            .comfortWeight(p.getComfortWeight()).safetyWeight(p.getSafetyWeight()).ecoWeight(p.getEcoWeight())
            .triggerReason(cut("Sugestao aceita: " + s.getReason(), 200))
            .version(p.getVersion()).build());

        p.setCostWeight(s.getCostWeight());
        p.setTimeWeight(s.getTimeWeight());
        p.setComfortWeight(s.getComfortWeight());
        p.setSafetyWeight(s.getSafetyWeight());
        p.setEcoWeight(s.getEcoWeight());
        p.setVersion(p.getVersion() + 1);
        preferenceRepository.save(p);

        s.setStatus("ACCEPTED");
        s.setResolvedAt(LocalDateTime.now());
        suggestionRepository.save(s);
        log.info("Usuario {} aceitou a sugestao {} (pesos v{})", userId, id, p.getVersion());
        return toMap(s);
    }

    @Transactional
    public Map<String, Object> reject(Long userId, Long id) {
        WeightSuggestion s = find(userId, id);
        s.setStatus("REJECTED");
        s.setResolvedAt(LocalDateTime.now());
        suggestionRepository.save(s);
        return toMap(s);
    }

    private WeightSuggestion find(Long userId, Long id) {
        WeightSuggestion s = suggestionRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Sugestao nao encontrada"));
        if (!"PENDING".equals(s.getStatus())) throw new IllegalStateException("Essa sugestao ja foi respondida");
        return s;
    }

    private Map<String, Object> toMap(WeightSuggestion s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("focus", s.getFocus());
        m.put("message", "COST".equals(s.getFocus())
            ? "Percebi que você costuma escolher opções mais baratas. Quer que eu passe a dar mais peso à economia?"
            : "Percebi que você costuma escolher opções mais rápidas. Quer que eu passe a dar mais peso ao tempo?");
        m.put("reason", s.getReason());
        m.put("status", s.getStatus());
        Map<String, Double> w = new LinkedHashMap<>();
        w.put("cost", s.getCostWeight()); w.put("time", s.getTimeWeight());
        w.put("comfort", s.getComfortWeight()); w.put("safety", s.getSafetyWeight()); w.put("eco", s.getEcoWeight());
        m.put("suggestedWeights", w);
        return m;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findOption(String cardJson, String mode) {
        try {
            Map<String, Object> card = objectMapper.readValue(cardJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> all = new ArrayList<>();
            if (card.get("recommended") instanceof Map<?, ?> r) all.add((Map<String, Object>) r);
            if (card.get("alternatives") instanceof List<?> l) for (Object o : l) if (o instanceof Map<?, ?> m) all.add((Map<String, Object>) m);
            for (Map<String, Object> o : all) if (mode.equals(o.get("mode"))) return o;
        } catch (Exception ignored) { }
        return null;
    }

    private double price(Object formatted) {
        if (formatted == null) return 0;
        String s = formatted.toString().replaceAll("[^0-9,]", "").replace(",", ".");
        try { return s.isEmpty() ? 0 : Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    private String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
