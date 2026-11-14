package com.smartway.api.application.service;

import com.smartway.api.adapter.out.persistence.ArchetypeRepository;
import com.smartway.api.adapter.out.persistence.UserPreferenceRepository;
import com.smartway.api.adapter.out.persistence.UserRepository;
import com.smartway.api.application.exception.DuplicateResourceException;
import com.smartway.api.application.exception.ResourceNotFoundException;
import com.smartway.api.domain.entity.Archetype;
import com.smartway.api.domain.entity.User;
import com.smartway.api.domain.entity.UserPreference;
import com.smartway.api.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final ArchetypeRepository archetypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;


    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Senha incorreta");
        }
        return tokenProvider.generateToken(user.getId(), user.getEmail());
    }

    /** Cadastro com perfil calculado pelo servidor a partir da triagem. */
    @Transactional
    public User registerWithProfile(String name, String email, String password, ProfilingService.Profile profile) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email ja cadastrado: " + email);
        }
        User user = userRepository.save(User.builder().name(name).email(email)
                .password(passwordEncoder.encode(password)).build());

        Archetype archetype = archetypeRepository.findByName(profile.archetype())
                .orElseThrow(() -> new ResourceNotFoundException("Arquetipo nao encontrado"));
        java.util.Map<String, Double> w = profile.weights();

        preferenceRepository.save(UserPreference.builder()
                .user(user).archetype(archetype)
                .costWeight(w.get("cost")).timeWeight(w.get("time"))
                .comfortWeight(w.get("comfort")).safetyWeight(w.get("safety")).ecoWeight(w.get("eco"))
                .availableModes(profile.availableModes().stream().map(Enum::name)
                        .collect(java.util.stream.Collectors.joining(",")))
                .priorities(String.join(",", profile.priorities()))
                .multimodal(profile.multimodal()).transitPreference(profile.transitPreference())
                .version(1).build());
        return user;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }
}
