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
        p.append("Voce e a Bibi, assistente do app SmartWay. Escreva UMA frase (ou duas curtas, se houver aviso de custo), com no maximo 35 palavras, em português do Brasil, com acentuação e crase corretas (ex: você, relação, ônibus). ")
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
         .append("'Indo de moto, você gasta uns 2 reais a menos de combustível do que de carro e chega praticamente no mesmo tempo. Só lembre que o estacionamento do shopping é cobrado à parte.' ")
         .append("Sempre diga EM QUE a pessoa economiza: combustível (carro e moto), passagem (ônibus e trem) ou corrida (aplicativo). Nunca diga apenas 'economiza X reais'. ")
         .append("Ligue as ideias em um texto corrido e natural, sem frases soltas. Pode usar no máximo duas frases curtas quando houver um aviso de estacionamento ou pedágio. ")
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
        String custos = costNote(rec, ctx);
        if (!custos.isEmpty()) p.append("Custos do carro: ").append(custos)
            .append(" Mencione de forma breve, sem inventar valores de estacionamento. ");
        p.append("Prioridade do usuario: ").append(req.getPriority() == null ? "equilibrio" : req.getPriority()).append(". ");
        if (ctx.getInsights() != null && !ctx.getInsights().isEmpty()) {
            p.append("Alertas: ");
            for (ContextInsight i : ctx.getInsights()) { if (second == null || i.getPenalizedModes() == null || !i.getPenalizedModes().contains(second.getMode())) continue; p.append(i.getTitle()); if (i.getPenalizedModes() != null && !i.getPenalizedModes().isEmpty()) { p.append(" (desfavorece: "); for (TransportMode m : i.getPenalizedModes()) p.append(label(m)).append(" "); p.append(")"); } p.append(". "); }
        }
        return p.toString();
    }

    // Usado quando a IA falha: monta UMA fala corrida, com o motivo explicito (combustivel, passagem, corrida)
    private String fallback(RouteOption rec, RouteOption second, ContextAnalysisResult ctx) {
        java.util.List<String> partes = new java.util.ArrayList<>();

        // 1. Contexto que decidiu (bar, tempestade, hospital) vem primeiro
        String critico = mainContext(ctx);
        // Sem contexto critico, usa o alerta que fez a 2a opcao perder (ex.: chuva para a bike).
        // Estacionamento fica de fora aqui: e tratado no aviso de custo, no fim da fala.
        if (critico.isEmpty() && ctx.getInsights() != null && second != null) {
            for (ContextInsight i : ctx.getInsights()) {
                if (i.getPenalizedModes() != null && i.getPenalizedModes().contains(second.getMode())
                        && i.getMessage() != null && !i.getMessage().toLowerCase().contains("estacion")) {
                    critico = i.getMessage(); break;
                }
            }
        }
        if (!critico.isEmpty()) partes.add(critico);

        // 2. Comparacao com a segunda opcao, dizendo em que se economiza
        if (unrealistic(rec, second)) {
            partes.add(String.format(new java.util.Locale("pt", "BR"),
                "São cerca de %.0f km, longe demais para ir %s, então assim você chega bem mais rápido.",
                Math.max(1, ctx.getDistanceKm()), second.getMode() == TransportMode.BICYCLE ? "de bicicleta" : "a pé"));
        } else if (second != null) {
            String c = comparison(rec, second, ctx);
            if (!c.isEmpty()) partes.add(c);
        }

        // 3. Avisos uteis (onibus e custos de quem dirige)
        String trecho = transitNote(rec);
        if (!trecho.isEmpty()) partes.add(trecho);
        String custo = costNoteUser(rec, ctx);
        if (!custo.isEmpty()) partes.add(custo);

        String texto = String.join(" ", partes).trim().replaceAll(" +", " ");
        return texto.isEmpty() ? "Essa é a opção que melhor combina com o seu perfil para esse trajeto." : texto;
    }

    // Ex.: "Indo de moto, você gasta uns 2 reais a menos de combustível do que de carro e chega praticamente no mesmo tempo."
    private String comparison(RouteOption rec, RouteOption second, ContextAnalysisResult ctx) {
        long economia = Math.round(second.getEstimatedCost() - rec.getEstimatedCost());
        int dt = rec.getEstimatedTimeMinutes() - second.getEstimatedTimeMinutes(); // >0: recomendado e mais lento
        String tempo = Math.abs(dt) <= 2 ? " e chega praticamente no mesmo tempo"
            : dt > 0 ? ", levando só uns " + dt + " minutos a mais"
            : " e ainda chega uns " + (-dt) + " minutos antes";
        String extra = (!drives(rec) && drives(second) && paidParking(ctx)) ? ", sem precisar pagar estacionamento" : "";

        if (economia >= 2) {
            String gasto = switch (rec.getMode()) {
                case CAR, MOTORCYCLE -> drives(second)
                    ? "você gasta uns " + economia + " reais a menos de combustível do que " + de(second.getMode())
                    : "o combustível sai uns " + economia + " reais mais barato do que " + preco(second.getMode());
                case BUS, TRAIN -> "a passagem sai uns " + economia + " reais mais barata do que " + preco(second.getMode());
                case RIDE_SHARE -> "a corrida sai uns " + economia + " reais mais barata do que " + preco(second.getMode());
                default -> "você não gasta nada e economiza uns " + economia + " reais em relação " + comArtigo(second.getMode());
            };
            return cap("Indo " + de(rec.getMode()) + ", " + gasto + tempo + extra + ".");
        }
        if (dt <= -3) {
            String custoExtra = economia <= -2 ? ", pagando uns " + (-economia) + " reais a mais" : "";
            return cap("Indo " + de(rec.getMode()) + ", você chega uns " + (-dt) + " minutos antes do que " + de(second.getMode()) + custoExtra + extra + ".");
        }
        if (!extra.isEmpty()) return cap("Indo " + de(rec.getMode()) + ", o custo fica parecido" + extra + ".");
        return "";
    }

    // "de moto", "de ônibus", "a pé"
    private String de(TransportMode m) {
        return switch (m) {
            case WALKING -> "a pé";
            case RIDE_SHARE -> "de aplicativo";
            default -> "de " + label(m);
        };
    }

    // Como chamar o preco da segunda opcao: "a corrida de aplicativo", "a passagem de ônibus", "ir de carro"
    private String preco(TransportMode m) {
        return switch (m) {
            case RIDE_SHARE -> "a corrida de aplicativo";
            case BUS -> "a passagem de ônibus";
            case TRAIN -> "a passagem de trem";
            default -> "ir " + de(m);
        };
    }

    private String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    // Pedagio entra no preco; estacionamento nao tem fonte de valor, entao so e avisado (sem numero)
    private boolean drives(RouteOption r) {
        return r != null && (r.getMode() == TransportMode.CAR || r.getMode() == TransportMode.MOTORCYCLE);
    }

    private boolean paidParking(ContextAnalysisResult ctx) {
        return ctx.getDestinationType() == com.smartway.api.domain.enums.DestinationType.SHOPPING_MALL
            || ctx.getDestinationType() == com.smartway.api.domain.enums.DestinationType.AIRPORT;
    }

    private String costNote(RouteOption rec, ContextAnalysisResult ctx) {
        if (!drives(rec)) return "";
        StringBuilder b = new StringBuilder();
        if (ctx.getTollCost() > 0) b.append("o preco ja inclui cerca de ").append(Math.round(ctx.getTollCost() * (rec.getMode() == TransportMode.MOTORCYCLE ? 0.5 : 1)))
            .append(" reais de pedagio. ");
        if (paidParking(ctx)) b.append("o destino costuma cobrar estacionamento, que NAO esta incluido no preco; avise isso. ");
        return b.toString();
    }

    private String costNoteUser(RouteOption rec, ContextAnalysisResult ctx) {
        if (!drives(rec)) return "";
        String s = "";
        if (ctx.getTollCost() > 0) s += "O valor já considera o pedágio do caminho. ";
        if (paidParking(ctx)) s += ctx.getDestinationType() == com.smartway.api.domain.enums.DestinationType.AIRPORT
            ? "Só lembre que o estacionamento do aeroporto é cobrado à parte."
            : "Só lembre que o estacionamento do shopping é cobrado à parte.";
        return s.trim();
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
