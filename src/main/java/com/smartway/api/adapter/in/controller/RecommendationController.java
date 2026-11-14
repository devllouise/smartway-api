package com.smartway.api.adapter.in.controller;

import com.smartway.api.adapter.in.dto.request.RecommendationRequest;
import com.smartway.api.application.service.RecommendationService;
import com.smartway.api.application.service.RouteHistoryService;
import com.smartway.api.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RouteHistoryService historyService;

    // O perfil vem do usuario logado; o app so manda origem e destino
    @PostMapping
    public ResponseEntity<Map<String, Object>> recommend(@AuthenticationPrincipal User user,
                                                         @Valid @RequestBody RecommendationRequest request) {
        log.info("Recebida requisicao: {} -> {}", request.getOrigin(), request.getDestination());
        RecommendationService.Result result = recommendationService.recommend(request,
            user != null ? user.getPreference() : null, user != null ? user.getId() : null);

        Map<String, Object> body = new LinkedHashMap<>();
        if (user != null) body.put("id", historyService.save(user.getId(), request, result.card(), result.context()));
        body.putAll(result.card());
        return ResponseEntity.ok(body);
    }
}
