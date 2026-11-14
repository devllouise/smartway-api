package com.smartway.api.adapter.out.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartway.api.domain.model.TransitRoute;
import com.smartway.api.port.out.TransitPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
public class GoogleTransitAdapter implements TransitPort {

    private static final String URL = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final String FIELDS = "routes.duration,routes.distanceMeters,routes.travelAdvisory.transitFare,"
        + "routes.legs.steps.travelMode,routes.legs.steps.staticDuration,"
        + "routes.legs.steps.transitDetails.transitLine.nameShort,routes.legs.steps.transitDetails.transitLine.name";

    @Value("${google.maps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleTransitAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TransitRoute> findRoute(String origin, String destination, String pref) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set("X-Goog-Api-Key", apiKey);
            h.set("X-Goog-FieldMask", FIELDS);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("origin", Map.of("address", origin));
            body.put("destination", Map.of("address", destination));
            body.put("travelMode", "TRANSIT");
            body.put("computeAlternativeRoutes", true);
            if ("FEWER_TRANSFERS".equals(pref)) body.put("transitPreferences", Map.of("routingPreference", "FEWER_TRANSFERS"));
            if ("COMFORT".equals(pref)) body.put("transitPreferences", Map.of("routingPreference", "LESS_WALKING"));

            log.info("Consultando Google Routes TRANSIT: {} -> {} (pref={})", origin, destination, pref);
            String resp = restTemplate.postForObject(URL, new HttpEntity<>(body, h), String.class);

            List<TransitRoute> routes = new ArrayList<>();
            for (JsonNode r : objectMapper.readTree(resp).path("routes")) {
                TransitRoute t = parse(r);
                if (t != null) routes.add(t);
            }
            if (routes.isEmpty()) {
                log.warn("Nenhuma rota de transporte publico encontrada");
                return Optional.empty();
            }
            Comparator<TransitRoute> order = "CHEAPER".equals(pref)
                ? Comparator.comparingDouble((TransitRoute t) -> t.fareKnown() ? t.fare() : Double.MAX_VALUE)
                    .thenComparingInt(TransitRoute::minutes)
                : Comparator.comparingInt(TransitRoute::minutes);
            TransitRoute best = routes.stream().min(order).get();
            log.info("Transporte publico: {} min, tarifa={}, trechos={}", best.minutes(),
                best.fareKnown() ? best.fare() : "n/d", best.legs());
            return Optional.of(best);
        } catch (Exception e) {
            log.error("Erro ao consultar Routes TRANSIT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // Junta caminhadas seguidas (o Google quebra por rua) e ignora rotas so a pe
    private TransitRoute parse(JsonNode r) {
        List<TransitRoute.Leg> legs = new ArrayList<>();
        int walkSec = 0;
        boolean hasBus = false;
        for (JsonNode step : r.path("legs").path(0).path("steps")) {
            int sec = seconds(step.path("staticDuration").asText("0s"));
            if ("TRANSIT".equals(step.path("travelMode").asText())) {
                if (walkSec > 0) { legs.add(new TransitRoute.Leg("WALK", null, mins(walkSec))); walkSec = 0; }
                JsonNode line = step.path("transitDetails").path("transitLine");
                String name = line.path("nameShort").asText(line.path("name").asText(""));
                legs.add(new TransitRoute.Leg("BUS", name.isBlank() ? null : name, mins(sec)));
                hasBus = true;
            } else {
                walkSec += sec;
            }
        }
        if (walkSec > 0) legs.add(new TransitRoute.Leg("WALK", null, mins(walkSec)));
        if (!hasBus) return null;

        JsonNode fare = r.path("travelAdvisory").path("transitFare");
        boolean fareKnown = fare.has("units") || fare.has("nanos");
        double value = fare.path("units").asDouble(0) + fare.path("nanos").asDouble(0) / 1_000_000_000.0;
        double km = Math.round(r.path("distanceMeters").asDouble(0) / 100.0) / 10.0;
        return new TransitRoute(mins(seconds(r.path("duration").asText("0s"))), value, fareKnown, km, legs);
    }

    private int seconds(String s) {
        try { return Integer.parseInt(s.replace("s", "").trim()); } catch (NumberFormatException e) { return 0; }
    }

    private int mins(int sec) {
        return Math.max(1, (int) Math.round(sec / 60.0));
    }
}
