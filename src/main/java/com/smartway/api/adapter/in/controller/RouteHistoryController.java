package com.smartway.api.adapter.in.controller;

import com.smartway.api.application.service.RouteHistoryService;
import com.smartway.api.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommendations/history")
@RequiredArgsConstructor
public class RouteHistoryController {

    private final RouteHistoryService service;

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal User user,
                                          @RequestParam(defaultValue = "20") int limit) {
        return service.list(user.getId(), limit);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.detail(user.getId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
