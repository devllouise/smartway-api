package com.smartway.api.adapter.out.ai;

import com.smartway.api.port.out.AiPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiAiAdapter implements AiPort {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${smartway.gemini.api-key}")
    private String apiKey;

    @Value("${smartway.gemini.model:gemini-3.8-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAiAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateExplanation(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 2048)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        try {
            log.info("Enviando prompt para Gemini {} ({} chars)", model, prompt.length());
            String response = restTemplate.postForObject(
                BASE_URL + model + ":generateContent", new HttpEntity<>(body, headers), String.class);

            JsonNode candidate = objectMapper.readTree(response).path("candidates").path(0);
            String finish = candidate.path("finishReason").asText("");

            // Junta todas as partes de texto, ignorando as de raciocinio ("thought")
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (part.path("thought").asBoolean(false)) continue;
                sb.append(part.path("text").asText(""));
            }
            String text = sb.toString().trim();

            log.info("Resposta Gemini: {} chars (finishReason={})", text.length(), finish);
            if (text.isBlank() || "MAX_TOKENS".equals(finish)) {
                throw new IllegalStateException("Resposta da IA vazia ou incompleta (" + finish + ")");
            }
            return text;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro Gemini API: {}", e.getClass().getSimpleName());
            throw new IllegalStateException("Falha ao consultar a IA", e);
        }
    }
}
