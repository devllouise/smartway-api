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
        p.append("Voce e a Bibi, assistente do app SmartWay. Escreva UMA frase, com no maximo 25 palavras, em português do Brasil, com acentuação e crase corretas (ex: você, relação, ônibus). ")
         .append("O app ja mostra o modal, o preco, o tempo e o horario de chegada: NAO repita nada disso e nao diga o nome do modal recomendado. ")
         .append("Explique o motivo REAL comparando com a segunda opcao (quanto economiza ou quanto tempo ganha) ")
         .append("Diga sempre com qual opcao esta comparando. So fale em economia se o recomendado for mais barato; se ele custar mais, explique o tempo que o usuario ganha. Se o recomendado for mais lento, cite de forma breve quantos minutos a mais leva, como uma troca justa pela economia. Nunca use um alerta para explicar diferenca de tempo ou custo. Nao cite modais que nao aparecem nos DADOS. Evite frases genericas como 'equilibrio adequado'. Use os alertas apenas para explicar por que as opcoes desfavorecidas perderam; nunca sugira ao usuario usar essas opcoes. ")
         .append("O app e usado por pessoas de todas as idades: tom gentil, claro e respeitoso, falando com 'voce'. ")
         .append("Nao use girias, diminutivos, emojis nem exclamacoes. Nao cumprimente, nao diga 'escolhi' nem 'recomendo', ")
         .append("nao fale de conforto e nao invente dados. Nao use expressoes como 'percorrer estes X km' ou 'mantendo o mesmo tempo'. Fale como uma pessoa explicaria para um amigo, de forma simples: evite linguagem de calculo como 'compensando', 'custo adicional', 'diferenca de' ou 'otimizar'. Arredonde numeros de forma natural (cerca de, uns, pouco mais de). Exemplos de tom: ")
         .append("'Você economiza cerca de 25 reais em relação ao aplicativo, e à noite é mais seguro do que ir de bicicleta.' ")
         .append("'Está chovendo, então essa é a forma mais segura de chegar. Você ainda chega meia hora antes do que de bicicleta.' ")
         .append("'O trajeto é curto e você não gasta nada para chegar.' ")
         .append("'São uns 7 km, longe demais para ir a pé, e de carro você chega em poucos minutos.' ")
         .append("'O ônibus leva um pouco mais, mas a viagem sai bem mais barata do que de aplicativo.' ")
         .append("'Como você vai para um bar, é melhor deixar o carro em casa e ir de aplicativo.' ")
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
        // Contexto que define a decisao (bar, tempestade, hospital): a Bibi deve citar primeiro
        String principal = mainContext(ctx);
        if (!principal.isEmpty()) p.append("CONTEXTO PRINCIPAL (comece a frase por ele, de forma natural e sem julgar o usuario): ")
            .append(principal).append(" Depois, se couber, complemente com a comparacao. ");
        p.append("Distancia do trajeto: ").append(String.format(java.util.Locale.US, "%.1f", ctx.getDistanceKm())).append(" km. ");
        if (unrealistic(rec, second)) p.append("A segunda opcao NAO e uma alternativa realista para esse trajeto (demora demais). ")
            .append("Nao faca conta de minutos nem de reais com ela: explique de forma natural por que a recomendada faz sentido, ")
            .append("usando a distancia ou a praticidade do trajeto. ");
        String trecho = transitNote(rec);
        if (!trecho.isEmpty()) p.append("Trajeto de onibus: ").append(trecho)
            .append(" Inclua essa informacao de forma breve e natural, como um aviso util, sem dramatizar. ");
        p.append("Prioridade do usuario: ").append(req.getPriority() == null ? "equilibrio" : req.getPriority()).append(". ");
        if (ctx.getInsights() != null && !ctx.getInsights().isEmpty()) {
            p.append("Alertas: ");
            for (ContextInsight i : ctx.getInsights()) { if (second == null || i.getPenalizedModes() == null || !i.getPenalizedModes().contains(second.getMode())) continue; p.append(i.getTitle()); if (i.getPenalizedModes() != null && !i.getPenalizedModes().isEmpty()) { p.append(" (desfavorece: "); for (TransportMode m : i.getPenalizedModes()) p.append(label(m)).append(" "); p.append(")"); } p.append(". "); }
        }
        return p.toString();
    }

    // Usado quando a IA falha: mesmo tom, sem repetir o que o card ja mostra
    private String fallback(RouteOption rec, RouteOption second, ContextAnalysisResult ctx) {
        String alerta = mainContext(ctx);
        if (alerta.isEmpty() && ctx.getInsights() != null && second != null) {
            for (ContextInsight i : ctx.getInsights()) {
                if (i.getPenalizedModes() != null && i.getPenalizedModes().contains(second.getMode())
                        && i.getMessage() != null) {
                    alerta = i.getMessage();
                    if (i.isHardRestriction() || "CRITICAL".equals(i.getSeverity())) break;
                }
            }
        }
        String troca = "";
        if (unrealistic(rec, second)) {
            troca = String.format(new java.util.Locale("pt", "BR"), "São cerca de %.0f km, uma distância longa para fazer %s, e assim você chega bem mais rápido.",
                Math.max(1, ctx.getDistanceKm()), second.getMode() == TransportMode.BICYCLE ? "de bicicleta" : "a pé");
        } else if (second != null) {
            long economia = Math.round(second.getEstimatedCost() - rec.getEstimatedCost());
            int ganho = second.getEstimatedTimeMinutes() - rec.getEstimatedTimeMinutes();
            if (economia >= 2) troca = "Você economiza cerca de " + economia + " reais em relação " + comArtigo(second.getMode()) + ".";
            else if (ganho >= 3) troca = "Você chega " + ganho + " minutos antes do que de " + label(second.getMode()) + ".";
        }
        String texto = (alerta + " " + troca + " " + transitNote(rec)).trim().replaceAll(" +", " ");
        return texto.isEmpty() ? "Essa é a opção com o melhor equilíbrio entre custo e tempo agora." : texto;
    }

    // Insight critico ou que bloqueou opcoes (ex: destino bar): e o motivo mais importante da decisao
    private String mainContext(ContextAnalysisResult ctx) {
        if (ctx.getInsights() == null) return "";
        for (ContextInsight i : ctx.getInsights())
            if (i.isHardRestriction() && i.getMessage() != null) return i.getMessage();
        for (ContextInsight i : ctx.getInsights())
            if ("CRITICAL".equals(i.getSeverity()) && i.getMessage() != null) return i.getMessage();
        return "";
    }

    // A 2a opcao so serve de comparacao se for uma alternativa de verdade (nao 1h a pe contra 10 min de carro)
    private boolean unrealistic(RouteOption rec, RouteOption second) {
        if (second == null) return false;
        int r = Math.max(1, rec.getEstimatedTimeMinutes()), s = second.getEstimatedTimeMinutes();
        return (second.getMode() == TransportMode.WALKING || second.getMode() == TransportMode.BICYCLE)
            && s >= 40 && s >= r * 3;
    }

    // Aviso sobre o onibus recomendado: baldeacao ou caminhada ate o ponto (dados reais da rota)
    private String transitNote(RouteOption rec) {
        if (rec.getMode() != TransportMode.BUS || rec.getLegs() == null || rec.getLegs().isEmpty()) return "";
        long buses = rec.getLegs().stream().filter(l -> "BUS".equals(l.type())).count();
        if (buses >= 2) return buses == 2 ? "O trajeto tem uma troca de ônibus no caminho."
            : "O trajeto tem " + (buses - 1) + " trocas de ônibus no caminho.";
        var legs = rec.getLegs();
        var first = legs.get(0); var last = legs.get(legs.size() - 1);
        if ("WALK".equals(first.type()) && first.minutes() >= 8)
            return "O ponto fica a uns " + first.minutes() + " minutos a pé.";
        if (legs.size() > 1 && "WALK".equals(last.type()) && last.minutes() >= 8)
            return "Depois de descer do ônibus, ainda são uns " + last.minutes() + " minutos a pé até o destino.";
        return "";
    }

    // "ao ônibus", "ao aplicativo", "à bicicleta"...
    private String comArtigo(TransportMode m) {
        return switch (m) {
            case BUS -> "ao ônibus";
            case CAR -> "ao carro";
            case RIDE_SHARE -> "ao aplicativo";
            case TRAIN -> "ao trem";
            case MOTORCYCLE -> "à moto";
            case BICYCLE -> "à bicicleta";
            case WALKING -> "à caminhada";
        };
    }

    private String label(TransportMode m) {
        return switch (m) {
            case CAR -> "carro";
            case MOTORCYCLE -> "moto";
            case BUS -> "ônibus";
            case TRAIN -> "trem";
            case RIDE_SHARE -> "aplicativo";
            case BICYCLE -> "bicicleta";
            case WALKING -> "caminhada";
        };
    }
}
