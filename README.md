# SmartWay API

Backend do **SmartWay**, assistente de mobilidade urbana que recomenda o melhor meio de transporte
para cada trajeto, considerando o perfil do usuário, o clima, o trânsito e o tipo de destino.
A assistente **Bibi** explica a decisão em linguagem natural.

TCC — Análise e Desenvolvimento de Sistemas, Fatec Praia Grande.

## Stack

Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Flyway · Spring Security (JWT) · Docker

Integrações: Google Routes (rota, trânsito e transporte público), Google Places, Google Weather e Google Gemini.

## Como rodar

1. Crie o `.env` a partir do `.env.example`:
   ```
   GOOGLE_MAPS_API_KEY=sua_chave
   GEMINI_API_KEY=sua_chave
   ```
   - No Google Cloud, ative: **Routes API**, **Places API (New)** e **Weather API**.
   - A chave do Gemini é criada em https://aistudio.google.com/apikey
2. Suba tudo:
   ```
   docker compose up --build -d
   ```
3. API em `http://localhost:8080/api/v1` · Swagger em `http://localhost:8080/swagger-ui.html`

## Como o motor decide (4 níveis)

| Nível | O que faz | Onde |
|---|---|---|
| 1. Contexto | Rota e distância reais, trânsito, clima na origem, tipo de destino, horário e transporte público real | `ContextAnalyzerService`, adapters Google |
| 2. Decisão | Nota de 0 a 10 por critério (custo, tempo, conforto, segurança, impacto ambiental), média ponderada pelos pesos do perfil e ajustes de contexto (chuva, noite, distância, baldeação) | `ScoringEngineService` |
| 3. Explicação | A Bibi explica a escolha já feita pelo motor — a IA não decide nem inventa dados | `IntelligenceEngineService` |
| 4. Aprendizado | Hábito por contexto vira um reforço leve; padrões no feedback geram uma **sugestão** de ajuste de pesos que o usuário aceita ou recusa | `LearningService` |

## Rotas

| Método | Rota | Token |
|---|---|---|
| GET | `/health` | não |
| POST | `/profiling/analyze` | não |
| POST | `/auth/register` | não |
| POST | `/auth/login` | não |
| GET / PUT | `/users/me` | sim |
| PUT | `/users/me/preferences` | sim |
| GET | `/users/me/suggestions` | sim |
| POST | `/users/me/suggestions/{id}/accept` · `/reject` | sim |
| POST | `/recommendations` | sim |
| GET | `/recommendations/history` · `/history/{id}` | sim |
| DELETE | `/recommendations/history/{id}` | sim |
| POST | `/feedback` | sim |
| GET / POST | `/places` · `/places/recent-destinations` | sim |
| DELETE | `/places/{id}` | sim |

## Triagem (onboarding)

```json
{
  "priorities": ["TIME", "COST"],
  "modes": ["PUBLIC_TRANSPORT", "RIDE_SHARE", "WALKING"],
  "multimodal": "IF_WORTH"
}
```

- `priorities` (até 2): `TIME`, `COST`, `SAFETY`, `PRACTICAL`, `ECO`
- `modes`: `PUBLIC_TRANSPORT`, `OWN_CAR`, `RIDE_SHARE`, `MOTORCYCLE`, `BICYCLE`, `WALKING`
- `multimodal`: `ANY`, `IF_WORTH`, `SINGLE`, `DEPENDS`

O arquétipo e os pesos são sempre calculados no servidor. O app envia só as respostas.
