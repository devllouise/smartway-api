package com.smartway.api.adapter.out.fipe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/** Catalogo de marcas, modelos e anos da tabela FIPE (API publica parallelum, v2). */
@Slf4j
@Component
@RequiredArgsConstructor
public class FipeAdapter {

    private final RestTemplate restTemplate;

    @Value("${smartway.vehicle.fipe-base-url:https://fipe.parallelum.com.br/api/v2}")
    private String baseUrl;

    private String type(String vehicleType) {
        return "MOTORCYCLE".equalsIgnoreCase(vehicleType) ? "motorcycles" : "cars";
    }

    public List<Map<String, Object>> brands(String vehicleType) {
        return list(baseUrl + "/" + type(vehicleType) + "/brands");
    }

    public List<Map<String, Object>> models(String vehicleType, String brand) {
        return list(baseUrl + "/" + type(vehicleType) + "/brands/" + brand + "/models");
    }

    public List<Map<String, Object>> years(String vehicleType, String brand, String model) {
        return list(baseUrl + "/" + type(vehicleType) + "/brands/" + brand + "/models/" + model + "/years");
    }

    /** Detalhe de um ano/modelo: brand, model, modelYear, fuel, codeFipe. Vazio se a FIPE nao responder. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detail(String vehicleType, String brand, String model, String year) {
        String url = baseUrl + "/" + type(vehicleType) + "/brands/" + brand + "/models/" + model + "/years/" + year;
        try {
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            return body != null ? body : Map.of();
        } catch (Exception e) {
            log.warn("FIPE indisponivel ({}): {}", url, e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(String url) {
        try {
            Object[] body = restTemplate.getForObject(url, Object[].class);
            List<Map<String, Object>> out = new ArrayList<>();
            if (body != null) for (Object o : body) if (o instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
            return out;
        } catch (Exception e) {
            log.warn("FIPE indisponivel ({}): {}", url, e.getMessage());
            return List.of();
        }
    }
}
