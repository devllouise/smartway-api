package com.smartway.api.application.service;

import com.smartway.api.adapter.in.dto.request.RecommendationRequest;
import com.smartway.api.domain.enums.TransportMode;
import com.smartway.api.domain.model.ContextAnalysisResult;
import com.smartway.api.domain.model.ContextInsight;
import com.smartway.api.domain.model.RouteOption;
import com.smartway.api.port.out.AiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceEngineService {

    private final AiPort aiPort;

    // Mantida para compatibilidade
    public String explain(RouteOption rec, ContextAnalysisResult ctx, RecommendationRequest req) {
        return explain(rec, null, ctx, req);
    }

    public String explain(RouteOption rec, RouteOption second, ContextAnalysisResult ctx, RecommendationRequest req) {
        try {
            String answer = aiPort.generateExplanation(buildPrompt(rec, second, ctx, req));
            if (answer == null || answer.isBlank()) return fallback(rec, second, ctx);
            return answer.trim();
        } catch (Exception e) {
            log.warn("IA indisponivel; usando explicacao por template ({})", e.getMessage());
            return fallback(rec, second, ctx);
        }
    }

    // A IA so EXPLICA: o modal ja foi escolhido pelo motor de scoring
    private String buildPrompt(RouteOption rec, RouteOption second, ContextAnalysisResult ctx, RecommendationRequest req) {
        StringBuilder p = new StringBuilder();
        p.append("Voce e a Bibi, assistente do app SmartWay. Escreva UMA frase, com no maximo 25 palavras, em portugues do Brasil. ")
         .append("O app ja mostra o modal, o preco, o tempo e o horario de chegada: NAO repita nada disso e nao diga o nome do modal recomendado. ")
         .append("Explique o motivo REAL comparando com a segunda opcao (quanto economiza ou quanto tempo ganha) ")
         .append("Diga sempre com qual opcao esta comparando. So fale em economia se o recomendado for mais barato; se ele custar mais, explique o tempo que o usuario ganha. Se o recomendado for mais lento, cite de forma breve quantos minutos a mais leva, como uma troca justa pela economia. Nunca use um alerta para explicar diferenca de tempo ou custo. Nao cite modais que nao aparecem nos DADOS. Evite frases genericas como 'equilibrio adequado'. Use os alertas apenas para explicar por que as opcoes desfavorecidas perderam; nunca sugira ao usuario usar essas opcoes. ")
         .append("O app e usado por pessoas de todas as idades: tom gentil, claro e respeitoso, falando com 'voce'. ")
         .append("Nao use girias, diminutivos, emojis nem exclamacoes. Nao cumprimente, nao diga 'escolhi' nem 'recomendo', ")
         .append("nao fale de conforto e nao invente dados. Exemplos de tom: ")
         .append("'Voce economiza cerca de 25 reais em relacao ao Uber, e a noite e mais seguro do que ir de bicicleta.' ")
         .append("'Esta chovendo, entao essa e a forma mais segura de chegar. Voce ainda chega meia hora antes do que de bicicleta.' ")
         .append("'O trajeto e curto e voce nao gasta nada para chegar.' ")
         .append("DADOS. Recomendado: ").append(label(rec.getMode()))
         .append(", ").append(Math.round(rec.getEstimatedCost())).append(" reais, ")
         .append(rec.getEstimatedTimeMinutes()).append(" min. ");
        if (second != null) {
            long diffCost = Math.round(second.getEstimatedCost() - rec.getEstimatedCost());
            int diffTime = second.getEstimatedTimeMinutes() - rec.getEstimatedTimeMinutes();
            p.append("Segunda opcao: ").append(label(second.getMode())).append(" (")
             .append(diffCost >= 0 ? diffCost + " reais mais cara" : (-diffCost) + " reais mais barata que o recomendado, ou seja, o recomendado custa MAIS e nao ha economia").append(", ")
             .append(diffTime >= 0 ? diffTime + " min mais lenta que o recomendado" : (-diffTime) + " min mais rapida que o recomendado").append("). ");
        }
        p.append("Prioridade do usuario: ").append(req.getPriority() == null ? "equilibrio" : req.getPriority()).append(". ");
        if (ctx.getInsights() != null && !ctx.getInsights().isEmpty()) {
            p.append("Alertas: ");
            for (ContextInsight i : ctx.getInsights()) { if (second == null || i.getPenalizedModes() == null || !i.getPenalizedModes().contains(second.getMode())) continue; p.append(i.getTitle()); if (i.getPenalizedModes() != null && !i.getPenalizedModes().isEmpty()) { p.append(" (desfavorece: "); for (TransportMode m : i.getPenalizedModes()) p.append(label(m)).append(" "); p.append(")"); } p.append(". "); }
        }
        return p.toString();
    }

    // Usado quando a IA falha: mesmo tom, sem repetir o que o card ja mostra
    private String fallback(RouteOption rec, RouteOption second, ContextAnalysisResult ctx) {
        String alerta = "";
        if (ctx.getInsights() != null) {
            for (ContextInsight i : ctx.getInsights()) {
                if ("RAIN_WARNING".equals(i.getId()) || "STORM_BLOCK".equals(i.getId())) {
                    alerta = "Esta chovendo, entao essa e a forma mais segura de chegar.";
                    break;
                }
                if (i.getId() != null && i.getId().contains("NIGHT")) {
                    alerta = "Ja escureceu, entao essa e a opcao mais segura para o trajeto.";
                }
            }
        }
        String troca = "";
        if (second != null) {
            long economia = Math.round(second.getEstimatedCost() - rec.getEstimatedCost());
            int ganho = second.getEstimatedTimeMinutes() - rec.getEstimatedTimeMinutes();
            if (economia >= 2) troca = "Voce economiza cerca de " + economia + " reais em relacao a " + label(second.getMode()) + ".";
            else if (ganho >= 3) troca = "Voce chega " + ganho + " minutos antes do que de " + label(second.getMode()) + ".";
        }
        String texto = (alerta + " " + troca).trim();
        return texto.isEmpty() ? "Essa e a opcao com o melhor equilibrio entre custo e tempo agora." : texto;
    }

    private String label(TransportMode m) {
        return switch (m) {
            case CAR -> "carro";
            case MOTORCYCLE -> "moto";
            case BUS -> "onibus";
            case TRAIN -> "trem";
            case UBER -> "Uber";
            case NINETY_NINE -> "99";
            case BICYCLE -> "bicicleta";
            case WALKING -> "caminhada";
        };
    }
}
