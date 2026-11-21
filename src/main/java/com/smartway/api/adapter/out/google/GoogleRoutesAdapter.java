package com.smartway.api.adapter.out.google;

import com.smartway.api.domain.enums.TrafficLevel;
import com.smartway.api.port.out.TrafficPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Slf4j
@Component
public class GoogleRoutesAdapter implements TrafficPort {

    private static final String ROUTES_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final double FALLBACK_DISTANCE_KM = 8.5;

    @Value("${google.maps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleRoutesAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RouteInfo getRouteInfo(String origin, String destination) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", "routes.duration,routes.staticDuration,routes.distanceMeters,routes.legs.startLocation,routes.travelAdvisory.tollInfo");

            Map<String, Object> body = Map.of(
                "origin", Map.of("address", origin),
                "destination", Map.of("address", destination),
                "routingPreference", "TRAFFIC_AWARE",
                "travelMode", "DRIVE",
                "extraComputations", java.util.List.of("TOLLS")
            );

            log.info("Consultando Google Routes API: {} -> {}", origin, destination);
            String response = restTemplate.postForObject(ROUTES_URL, new HttpEntity<>(body, headers), String.class);
            JsonNode route = objectMapper.readTree(response).path("routes").path(0);

            if (route.isMissingNode() || route.isEmpty()) {
                log.warn("Nenhuma rota encontrada; usando valores estimados");
                return fallback();
            }

            long meters = route.path("distanceMeters").asLong(0);
            boolean estimated = meters <= 0;
            double km = estimated ? FALLBACK_DISTANCE_KM : Math.round(meters / 100.0) / 10.0;

            long duration = parseSeconds(route.path("duration").asText("0s"));
            long staticDuration = parseSeconds(route.path("staticDuration").asText("0s"));
            TrafficLevel level = classify(duration, staticDuration);

            log.info("Rota: {} km, transito={} (duration={}s, static={}s)", km, level, duration, staticDuration);
            double toll = parseToll(route.path("travelAdvisory").path("tollInfo"));
            if (toll > 0) log.info("Pedagio estimado na rota: R$ {}", toll);
            JsonNode ll = route.path("legs").path(0).path("startLocation").path("latLng");
            if (ll.has("latitude") && ll.has("longitude")) {
                return new RouteInfo(level, km, estimated, ll.get("latitude").asDouble(), ll.get("longitude").asDouble(), toll);
            }
            return new RouteInfo(level, km, estimated, -24.0058, -46.4028, toll);

        } catch (Exception e) {
            log.error("Erro ao consultar Google Routes API: {}", e.getMessage());
            return fallback();
        }
    }

    // tollInfo.estimatedPrice: [{ currencyCode, units, nanos }]. So considera valores em reais.
    private double parseToll(JsonNode tollInfo) {
        double total = 0;
        for (JsonNode price : tollInfo.path("estimatedPrice")) {
            if (!"BRL".equals(price.path("currencyCode").asText("BRL"))) continue;
            total += price.path("units").asDouble(0) + price.path("nanos").asDouble(0) / 1_000_000_000.0;
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private RouteInfo fallback() {
        return new RouteInfo(TrafficLevel.MODERATE, FALLBACK_DISTANCE_KM, true);
    }

    private long parseSeconds(String s) {
        try {
            return Long.parseLong(s.replace("s", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private TrafficLevel classify(long duration, long staticDuration) {
        if (staticDuration <= 0) return TrafficLevel.MODERATE;
        double ratio = (double) duration / staticDuration;
        if (ratio >= 1.8) return TrafficLevel.VERY_HIGH;
        if (ratio >= 1.4) return TrafficLevel.HIGH;
        if (ratio >= 1.15) return TrafficLevel.MODERATE;
        return TrafficLevel.LOW;
    }
}
