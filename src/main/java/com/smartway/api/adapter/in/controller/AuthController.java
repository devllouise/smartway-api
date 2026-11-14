package com.smartway.api.adapter.in.controller;

import com.smartway.api.adapter.in.dto.request.LoginRequest;
import com.smartway.api.adapter.in.dto.request.RegisterRequest;
import com.smartway.api.adapter.in.dto.response.AuthResponse;
import com.smartway.api.application.service.AuthService;
import com.smartway.api.application.service.ProfilingService;
import com.smartway.api.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final ProfilingService profilingService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterRequest.Profiling p = request.getProfiling();
        ProfilingService.Profile profile = profilingService.analyze(
                p != null ? p.getPriorities() : List.of(),
                p != null ? p.getModes() : List.of(),
                p != null ? p.getMultimodal() : null);

        User user = authService.registerWithProfile(request.getName(), request.getEmail(),
                request.getPassword(), profile);
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder().token(token).name(user.getName())
                        .email(user.getEmail()).archetype(profile.displayName()).build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        User user = authService.findByEmail(request.getEmail());
        String archetype = (user.getPreference() != null && user.getPreference().getArchetype() != null)
                ? profilingService.displayName(user.getPreference().getArchetype().getName()) : null;
        return ResponseEntity.ok(AuthResponse.builder().token(token).name(user.getName())
                .email(user.getEmail()).archetype(archetype).build());
    }
}
