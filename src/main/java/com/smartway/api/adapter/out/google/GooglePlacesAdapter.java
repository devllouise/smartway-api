package com.smartway.api.adapter.out.google;

import com.smartway.api.domain.enums.DestinationType;
import com.smartway.api.port.out.PlacesPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class GooglePlacesAdapter implements PlacesPort {

    private static final String PLACES_URL = "https://places.googleapis.com/v1/places:searchText";

    @Value("${google.maps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GooglePlacesAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public DestinationType getDestinationType(String destination) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", "places.types,places.displayName");

            Map<String, Object> body = Map.of("textQuery", destination);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Consultando Google Places API: {}", destination);
            String response = restTemplate.postForObject(PLACES_URL, entity, String.class);
            JsonNode root = objectMapper.readTree(response);

            JsonNode places = root.path("places");
            if (places.isEmpty() || !places.isArray() || places.size() == 0) {
                log.warn("Nenhum lugar encontrado para: {}", destination);
                return inferFromName(destination);
            }

            JsonNode types = places.get(0).path("types");
            if (types.isArray()) {
                Set<String> typeSet = new HashSet<>();
                types.forEach(t -> typeSet.add(t.asText()));

                log.info("Tipos encontrados: {}", typeSet);

                if (typeSet.contains("airport")) return DestinationType.AIRPORT;
                if (typeSet.contains("hospital") || typeSet.contains("doctor")) return DestinationType.HOSPITAL;
                if (typeSet.contains("bar") || typeSet.contains("night_club")) return DestinationType.BAR;
                if (typeSet.contains("restaurant") || typeSet.contains("food")) return DestinationType.RESTAURANT;
                if (typeSet.contains("shopping_mall")) return DestinationType.SHOPPING_MALL;
                if (typeSet.contains("university") || typeSet.contains("school")) return DestinationType.UNIVERSITY;
                if (typeSet.contains("gym")) return DestinationType.GYM;
                if (typeSet.contains("park")) return DestinationType.PARK;
            }

            return inferFromName(destination);

        } catch (Exception e) {
            log.error("Erro ao consultar Google Places API: {}", e.getMessage());
            return inferFromName(destination);
        }
    }

    private DestinationType inferFromName(String destination) {
        if (destination == null) return DestinationType.UNKNOWN;
        String d = destination.toLowerCase();
        if (has(d, "aeroporto") || has(d, "airport") || has(d, "congonhas")) return DestinationType.AIRPORT;
        if (has(d, "hospital") || has(d, "upa")) return DestinationType.HOSPITAL;
        if (has(d, "bar") || has(d, "pub") || has(d, "balada")) return DestinationType.BAR;
        if (has(d, "restaurante") || has(d, "pizzaria")) return DestinationType.RESTAURANT;
        if (has(d, "shopping") || has(d, "brisamar")) return DestinationType.SHOPPING_MALL;
        if (has(d, "fatec") || has(d, "universidade") || has(d, "faculdade")) return DestinationType.UNIVERSITY;
        if (has(d, "academia") || has(d, "smartfit")) return DestinationType.GYM;
        if (has(d, "parque") || has(d, "praia")) return DestinationType.PARK;
        return DestinationType.UNKNOWN;
    }

    // Fallback: compara palavras inteiras, somente no nome do lugar (antes da 1a virgula)
    private static boolean has(String d, String word) {
        String name = d.split(",")[0];
        return java.util.regex.Pattern.compile(
            "\\b" + java.util.regex.Pattern.quote(word) + "\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CHARACTER_CLASS
        ).matcher(name).find();
    }
}
