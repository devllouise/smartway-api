package com.smartway.api.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartway.api.adapter.in.dto.request.RecommendationRequest;
import com.smartway.api.adapter.out.persistence.RouteHistoryRepository;
import com.smartway.api.application.exception.ResourceNotFoundException;
import com.smartway.api.domain.entity.RouteHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteHistoryService {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm");

    private final RouteHistoryRepository repository;
    private final ObjectMapper objectMapper;

    /** Salva a busca. Se falhar, so loga: a recomendacao nunca deixa de ser entregue. */
    @SuppressWarnings("unchecked")
    public Long save(Long userId, RecommendationRequest req, Map<String, Object> card) {
        try {
            Map<String, Object> rec = (Map<String, Object>) card.get("recommended");
            RouteHistory h = RouteHistory.builder()
                .userId(userId).origin(req.getOrigin()).destination(req.getDestination())
                .recommendedMode(str(rec, "mode")).recommendedLabel(str(rec, "label"))
                .priceFormatted(str(rec, "priceFormatted"))
                .durationMinutes(rec != null && rec.get("durationMinutes") instanceof Number n ? n.intValue() : null)
                .bibiMessage(cut(str(rec, "bibiMessage"), 600))
                .cardJson(objectMapper.writeValueAsString(card))
                .build();
            return repository.save(h).getId();
        } catch (Exception e) {
            log.error("Nao foi possivel salvar o historico: {}", e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long userId, int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> out = new ArrayList<>();
        for (RouteHistory h : repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, size))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("origin", h.getOrigin());
            m.put("destination", h.getDestination());
            m.put("mode", h.getRecommendedMode());
            m.put("label", h.getRecommendedLabel());
            m.put("priceFormatted", h.getPriceFormatted());
            m.put("durationMinutes", h.getDurationMinutes());
            m.put("createdAt", h.getCreatedAt().format(BR));
            out.add(m);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long userId, Long id) {
        RouteHistory h = find(userId, id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("origin", h.getOrigin());
        m.put("destination", h.getDestination());
        m.put("createdAt", h.getCreatedAt().format(BR));
        try {
            m.put("card", objectMapper.readValue(h.getCardJson(), new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            m.put("card", null);
        }
        return m;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(find(userId, id));
    }

    // Busca sempre filtrando pelo dono: ninguem acessa o historico de outra pessoa
    private RouteHistory find(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Historico nao encontrado"));
    }

    private String str(Map<String, Object> m, String k) {
        return m == null || m.get(k) == null ? null : String.valueOf(m.get(k));
    }

    private String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
