package com.smartway.api.application.service;

import com.smartway.api.adapter.out.persistence.ArchetypeRepository;
import com.smartway.api.adapter.out.persistence.PreferenceHistoryRepository;
import com.smartway.api.adapter.out.persistence.UserPreferenceRepository;
import com.smartway.api.adapter.out.persistence.UserRepository;
import com.smartway.api.application.exception.ResourceNotFoundException;
import com.smartway.api.domain.entity.Archetype;
import com.smartway.api.domain.entity.PreferenceHistory;
import com.smartway.api.domain.entity.User;
import com.smartway.api.domain.entity.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final ArchetypeRepository archetypeRepository;
    private final PreferenceHistoryRepository preferenceHistoryRepository;
    private final ProfilingService profilingService;

    @Transactional(readOnly = true)
    public Map<String, Object> me(Long userId) {
        return toResponse(load(userId));
    }

    @Transactional
    public Map<String, Object> updateName(Long userId, String name) {
        if (name == null || name.isBlank()) throw new IllegalStateException("Nome e obrigatorio");
        User user = load(userId);
        user.setName(name.trim());
        userRepository.save(user);
        return toResponse(user);
    }

    // Refaz a triagem: o back recalcula arquetipo, pesos e modais (o app nunca envia o perfil)
    @Transactional
    public Map<String, Object> updatePreferences(Long userId, List<String> priorities, List<String> modes,
                                                 String multimodal) {
        User user = load(userId);
        ProfilingService.Profile profile = profilingService.analyze(priorities, modes, multimodal);
        Archetype archetype = archetypeRepository.findByName(profile.archetype())
                .orElseThrow(() -> new ResourceNotFoundException("Arquetipo nao encontrado"));
        Map<String, Double> w = profile.weights();

        UserPreference pref = user.getPreference();
        if (pref == null) {
            pref = UserPreference.builder().user(user).version(0).build();
            user.setPreference(pref);
        } else {
            // Guarda a versao anterior antes de mudar (rastreabilidade do perfil)
            preferenceHistoryRepository.save(PreferenceHistory.builder().user(user)
                .costWeight(pref.getCostWeight()).timeWeight(pref.getTimeWeight())
                .comfortWeight(pref.getComfortWeight()).safetyWeight(pref.getSafetyWeight())
                .ecoWeight(pref.getEcoWeight()).triggerReason("Triagem refeita pelo usuario")
                .version(pref.getVersion()).build());
        }
        pref.setArchetype(archetype);
        pref.setCostWeight(w.get("cost"));
        pref.setTimeWeight(w.get("time"));
        pref.setComfortWeight(w.get("comfort"));
        pref.setSafetyWeight(w.get("safety"));
        pref.setEcoWeight(w.get("eco"));
        pref.setAvailableModes(profile.availableModes().stream().map(Enum::name).collect(Collectors.joining(",")));
        pref.setPriorities(String.join(",", profile.priorities()));
        pref.setMultimodal(profile.multimodal());
        pref.setTransitPreference(profile.transitPreference());
        pref.setVersion(pref.getVersion() + 1);
        pref.setUpdatedAt(LocalDateTime.now());
        preferenceRepository.save(pref);
        return toResponse(user);
    }

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }

    private Map<String, Object> toResponse(User user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", user.getName());
        body.put("email", user.getEmail());
        UserPreference p = user.getPreference();
        String code = (p != null && p.getArchetype() != null) ? p.getArchetype().getName() : "EQUILIBRADO";

        Map<String, Object> archetype = new LinkedHashMap<>();
        archetype.put("code", code);
        archetype.put("displayName", profilingService.displayName(code));
        archetype.put("description", p != null && p.getArchetype() != null ? p.getArchetype().getDescription() : null);
        archetype.put("traits", profilingService.traits(code));
        body.put("archetype", archetype);

        body.put("priorities", split(p == null ? null : p.getPriorities()));
        body.put("availableModes", split(p == null ? null : p.getAvailableModes()));
        body.put("multimodal", p == null ? null : p.getMultimodal());
        body.put("transitPreference", p == null ? null : p.getTransitPreference());
        return body;
    }

    private List<String> split(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
