package com.smartway.api.application.service;

import com.smartway.api.adapter.out.persistence.SavedPlaceRepository;
import com.smartway.api.application.exception.DuplicateResourceException;
import com.smartway.api.application.exception.ResourceNotFoundException;
import com.smartway.api.domain.entity.SavedPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SavedPlaceService {

    private static final Set<String> CATEGORIES = Set.of("HOME", "WORK", "GYM", "CAFE", "SHOPPING", "OTHER");
    private final SavedPlaceRepository repository;

    public List<Map<String, Object>> list(Long userId, String q) {
        String term = q == null ? "" : q.trim().toLowerCase();
        List<Map<String, Object>> out = new ArrayList<>();
        for (SavedPlace p : repository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (!term.isEmpty() && !(p.getName() + " " + p.getAddress()).toLowerCase().contains(term)) continue;
            out.add(toMap(p));
        }
        return out;
    }

    public Map<String, Object> create(Long userId, String name, String address, String category) {
        if (address == null || address.isBlank()) throw new IllegalStateException("Endereco e obrigatorio");
        if (repository.existsByUserIdAndAddressIgnoreCase(userId, address.trim()))
            throw new DuplicateResourceException("Esse lugar ja esta nos seus salvos");
        String cat = category == null ? "OTHER" : category.trim().toUpperCase();
        SavedPlace p = repository.save(SavedPlace.builder().userId(userId)
            .name(name == null || name.isBlank() ? address.split(",")[0].trim() : name.trim())
            .address(address.trim()).category(CATEGORIES.contains(cat) ? cat : "OTHER").build());
        return toMap(p);
    }

    public void delete(Long userId, Long id) {
        repository.delete(repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Lugar nao encontrado")));
    }

    // "Trajetos recentes" da tela Novo lugar: destinos do historico que ainda nao foram salvos
    public List<Map<String, Object>> recentDestinations(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String d : repository.recentDestinations(userId)) {
            if (repository.existsByUserIdAndAddressIgnoreCase(userId, d)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.split(",")[0].trim());
            m.put("address", d);
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> toMap(SavedPlace p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName());
        m.put("address", p.getAddress()); m.put("category", p.getCategory());
        return m;
    }
}
