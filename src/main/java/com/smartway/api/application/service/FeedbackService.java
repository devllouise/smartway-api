package com.smartway.api.application.service;

import com.smartway.api.adapter.out.persistence.RouteFeedbackRepository;
import com.smartway.api.adapter.out.persistence.RouteHistoryRepository;
import com.smartway.api.application.exception.ResourceNotFoundException;
import com.smartway.api.domain.entity.RouteFeedback;
import com.smartway.api.domain.entity.RouteHistory;
import com.smartway.api.domain.enums.TransportMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final RouteFeedbackRepository feedbackRepository;
    private final RouteHistoryRepository historyRepository;
    private final LearningService learningService;

    @Transactional
    public Map<String, Object> submit(Long userId, Long historyId, Boolean helpful, String chosenMode, String comment) {
        RouteHistory h = historyRepository.findByIdAndUserId(historyId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Recomendacao nao encontrada no seu historico"));

        String mode = normalizeMode(chosenMode);
        if (mode == null && Boolean.TRUE.equals(helpful)) mode = h.getRecommendedMode();

        // Um feedback por busca: se mandar de novo, atualiza
        RouteFeedback f = feedbackRepository.findByHistoryId(historyId)
            .orElseGet(() -> RouteFeedback.builder().userId(userId).historyId(historyId).build());
        f.setHelpful(helpful);
        f.setChosenMode(mode);
        f.setComment(comment == null ? null : comment.length() > 1000 ? comment.substring(0, 1000) : comment);
        f = feedbackRepository.save(f);

        learningService.evaluate(userId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", f.getId());
        body.put("message", "Obrigado! A Bibi usa o seu retorno para melhorar as próximas recomendações.");
        body.put("suggestion", learningService.pending(userId).orElse(null));
        return body;
    }

    private String normalizeMode(String m) {
        if (m == null || m.isBlank()) return null;
        try {
            return TransportMode.valueOf(m.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Modal invalido: " + m);
        }
    }
}
