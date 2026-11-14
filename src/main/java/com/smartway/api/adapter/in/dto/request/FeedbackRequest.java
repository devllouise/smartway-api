package com.smartway.api.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {
    /** id devolvido na resposta da recomendacao (item do historico) */
    @NotNull(message = "historyId e obrigatorio")
    private Long historyId;
    /** true = 👍, false = 👎 */
    private Boolean helpful;
    /** Modal que o usuario realmente usou (opcional; se 👍 e vazio, assume o recomendado) */
    private String chosenMode;
    private String comment;
}
