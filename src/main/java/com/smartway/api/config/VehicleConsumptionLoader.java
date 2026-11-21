package com.smartway.api.config;

import com.smartway.api.adapter.out.persistence.VehicleConsumptionRepository;
import com.smartway.api.domain.entity.VehicleConsumption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Importa a tabela do PBE Veicular (Inmetro) de resources/data/pbev.csv na primeira inicializacao.
 * Formato: marca;modelo;versao;ano;combustivel;cidade_km_l;estrada_km_l
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleConsumptionLoader implements ApplicationRunner {

    private final VehicleConsumptionRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;
        ClassPathResource file = new ClassPathResource("data/pbev.csv");
        if (!file.exists()) { log.warn("data/pbev.csv nao encontrado: consumo padrao sera usado"); return; }
        List<VehicleConsumption> rows = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line; boolean header = true;
            while ((line = r.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] c = line.split(";", -1);
                if (c.length < 7 || c[0].isBlank() || c[1].isBlank()) continue;
                rows.add(VehicleConsumption.builder()
                    .brand(c[0].trim()).model(c[1].trim()).version(c[2].trim())
                    .modelYear(parseInt(c[3])).fuel(c[4].trim())
                    .cityKmPerLiter(parseDouble(c[5])).highwayKmPerLiter(parseDouble(c[6]))
                    .source("INMETRO PBEV").build());
            }
            repository.saveAll(rows);
            log.info("Tabela PBEV importada: {} modelos", rows.size());
        } catch (Exception e) {
            log.warn("Falha ao importar data/pbev.csv: {}", e.getMessage());
        }
    }

    private Integer parseInt(String v) { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return null; } }
    private Double parseDouble(String v) { try { return Double.parseDouble(v.trim().replace(",", ".")); } catch (Exception e) { return null; } }
}
