package com.smartway.api.adapter.in.controller;

import com.smartway.api.application.service.VehicleService;
import com.smartway.api.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService service;

    // Catalogo FIPE: type = CAR ou MOTORCYCLE
    @GetMapping("/catalog/brands")
    public List<Map<String, Object>> brands(@RequestParam(defaultValue = "CAR") String type) {
        return service.brands(type);
    }

    @GetMapping("/catalog/brands/{brand}/models")
    public List<Map<String, Object>> models(@RequestParam(defaultValue = "CAR") String type, @PathVariable String brand) {
        return service.models(type, brand);
    }

    @GetMapping("/catalog/brands/{brand}/models/{model}/years")
    public List<Map<String, Object>> years(@RequestParam(defaultValue = "CAR") String type,
                                           @PathVariable String brand, @PathVariable String model) {
        return service.years(type, brand, model);
    }

    // Consumo estimado pelos codigos da FIPE (o app so repete o que recebeu nas listas)
    @GetMapping("/catalog/brands/{brand}/models/{model}/years/{year}/estimate")
    public Map<String, Object> estimateByCodes(@RequestParam(defaultValue = "CAR") String type,
                                               @PathVariable String brand, @PathVariable String model,
                                               @PathVariable String year) {
        return service.estimateByCodes(type, brand, model, year);
    }

    // Alternativa por nome (uso em testes)
    @GetMapping("/estimate")
    public Map<String, Object> estimate(@RequestParam(defaultValue = "CAR") String type,
                                        @RequestParam String brandName, @RequestParam String modelName,
                                        @RequestParam(required = false) String fuel) {
        return service.estimate(type, brandName, modelName, fuel);
    }

    // Veiculos do usuario
    @GetMapping("/me")
    public List<Map<String, Object>> mine(@AuthenticationPrincipal User user) {
        return service.list(user.getId());
    }

    @PutMapping("/me")
    public Map<String, Object> save(@AuthenticationPrincipal User user, @RequestBody VehicleService.VehicleRequest req) {
        return service.save(user.getId(), req);
    }

    @DeleteMapping("/me/{type}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable String type) {
        service.delete(user.getId(), type);
        return ResponseEntity.noContent().build();
    }
}
