package com.smartway.api.adapter.in.controller;

import com.smartway.api.port.out.AiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiPort aiPort;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        log.info("Chat recebido: {}", message);

        String prompt = "Voce e a Bibi, assistente virtual do SmartWay. "
            + "Responda de forma curta, amigavel e em portugues brasileiro. "
            + "Pergunta do usuario: " + message;

        String response = aiPort.generateExplanation(prompt);

        return ResponseEntity.ok(Map.of(
            "response", response,
            "from", "Bibi"
        ));
    }
}