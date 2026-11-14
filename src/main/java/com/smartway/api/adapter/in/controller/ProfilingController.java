package com.smartway.api.adapter.in.controller;

import com.smartway.api.application.service.ProfilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiling")
@RequiredArgsConstructor
public class ProfilingController {

    private final ProfilingService profilingService;

    public record ProfilingRequest(List<String> priorities, List<String> modes,
                                   String multimodal) {}

    // Publica: usada na tela "Seu perfil esta pronto", antes de a conta existir. Nao salva nada.
    @PostMapping("/analyze")
    public ProfilingService.Profile analyze(@RequestBody ProfilingRequest req) {
        return profilingService.analyze(req.priorities(), req.modes(), req.multimodal());
    }
}
