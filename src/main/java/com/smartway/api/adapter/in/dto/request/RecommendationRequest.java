package com.smartway.api.adapter.in.dto.request;

import com.smartway.api.domain.enums.TransportMode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {

    @NotBlank(message = "Origem e obrigatoria")
    private String origin;

    @NotBlank(message = "Destino e obrigatorio")
    private String destination;

    /** Opcional: ajuste so para esta busca (COST, TIME ou COMFORT). Sem ele vale o perfil. */
    private String priority;

    /** Opcional: restringe os modais desta busca (dentro dos modais do perfil). */
    private List<TransportMode> preferredModes;
}
