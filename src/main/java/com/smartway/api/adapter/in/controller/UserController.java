package com.smartway.api.adapter.in.controller;

import com.smartway.api.application.service.UserProfileService;
import com.smartway.api.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService service;

    public record UpdateNameRequest(String name) {}
    public record PreferencesRequest(List<String> priorities, List<String> modes) {}

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
        return service.updatePreferences(user.getId(), req.priorities(), req.modes());
    }
}
