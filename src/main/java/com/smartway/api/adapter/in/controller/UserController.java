package com.smartway.api.adapter.in.controller;

import com.smartway.api.application.service.LearningService;
import com.smartway.api.application.service.UserProfileService;
import com.smartway.api.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService service;
    private final LearningService learningService;

    public record UpdateNameRequest(String name) {}
    public record PreferencesRequest(List<String> priorities, List<String> modes,
                                     String multimodal) {}

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal User user) {
        return service.me(user.getId());
    }

    @PutMapping
    public Map<String, Object> update(@AuthenticationPrincipal User user, @RequestBody UpdateNameRequest req) {
        return service.updateName(user.getId(), req.name());
    }

    @PutMapping("/preferences")
    public Map<String, Object> preferences(@AuthenticationPrincipal User user, @RequestBody PreferencesRequest req) {
        return service.updatePreferences(user.getId(), req.priorities(), req.modes(),
            req.multimodal());
    }

    // Nivel 4: sugestao de ajuste de pesos gerada pelo aprendizado (o usuario decide)
    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> suggestion(@AuthenticationPrincipal User user) {
        return learningService.pending(user.getId())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/suggestions/{id}/accept")
    public Map<String, Object> accept(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return learningService.accept(user.getId(), id);
    }

    @PostMapping("/suggestions/{id}/reject")
    public Map<String, Object> reject(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return learningService.reject(user.getId(), id);
    }
}
