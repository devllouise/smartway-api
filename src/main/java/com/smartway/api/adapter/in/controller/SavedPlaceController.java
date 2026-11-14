package com.smartway.api.adapter.in.controller;

import com.smartway.api.application.service.SavedPlaceService;
import com.smartway.api.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class SavedPlaceController {

    private final SavedPlaceService service;
    public record PlaceRequest(String name, String address, String category) {}

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal User user, @RequestParam(required = false) String q) {
        return service.list(user.getId(), q);
    }

    @GetMapping("/recent-destinations")
    public List<Map<String, Object>> recent(@AuthenticationPrincipal User user) {
        return service.recentDestinations(user.getId());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@AuthenticationPrincipal User user, @RequestBody PlaceRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(), r.name(), r.address(), r.category()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
