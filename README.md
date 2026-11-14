# SmartWay API v2.0

Assistente Virtual Inteligente para Mobilidade Urbana

## Sobre

O SmartWay e um assistente virtual que ajuda usuarios a tomar decisoes inteligentes sobre mobilidade urbana.

- Calcula o custo REAL do carro (combustivel + desgaste + estacionamento)
- Compara TODOS os modais de transporte lado a lado
- Considera contexto: clima, transito, tipo de destino, horario
- Aprende com o usuario e evolui as recomendacoes
- Explica o raciocinio atraves da Bibi (assistente IA)

## Arquitetura - 4 Niveis de Inteligencia

- Nivel 1 COLETA: ContextAnalyzerService (clima, transito, destino, padroes)
- Nivel 2 PONTUACAO: ScoringEngineService (custo x tempo x conforto x seguranca x eco)
- Nivel 3 DECISAO: IntelligenceEngineService (recomendacao + explicacao da Bibi)
- Nivel 4 APRENDIZADO: LearningService (feedback loop + auto-ajuste de pesos)

## Stack Tecnologica

- Java 21
- Spring Boot 3
- PostgreSQL
- Docker + Docker Compose
- JWT (autenticacao)
- OpenWeather API (dados climaticos)
- OpenAI API (explicacoes da Bibi)
- Swagger/OpenAPI (documentacao)

## Como Rodar

### Pre-requisitos
- Docker e Docker Compose
- Java 21 (para desenvolvimento)

### Subir com Docker
docker compose up --build

API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html

### Variaveis de Ambiente (opcionais)
SMARTWAY_WEATHER_API_KEY=sua_chave_openweather
SMARTWAY_OPENAI_API_KEY=sua_chave_openai
Sem essas chaves, o sistema usa fallbacks inteligentes.

## Endpoints

### Autenticacao
- POST /api/auth/register - Criar conta
- POST /api/auth/login - Login (JWT)

### Recomendacao
- POST /api/recommendations - Pedir recomendacao de rota

### Feedback
- POST /api/feedback - Enviar feedback

### Chat (Bibi)
- POST /api/chat - Conversar com a Bibi

### Health
- GET /api/health - Status da API

## Exemplo de Uso

### 1. Registrar
POST /api/auth/register
{
  "name": "Maira",
  "email": "maira@email.com",
  "password": "123456",
  "archetype": "ECONOMICO"
}

### 2. Pedir Recomendacao
POST /api/recommendations
Authorization: Bearer {token}
{
  "originAddress": "Fatec Praia Grande",
  "originLat": -23.9618,
  "originLng": -46.3922,
  "destinationAddress": "Shopping Praia Mar",
  "destinationLat": -23.9712,
  "destinationLng": -46.4015,
  "intent": "SHOPPING"
}

## Arquetipos de Usuario

- ECONOMICO: custo 0.40, tempo 0.20, conforto 0.10, seguranca 0.20, eco 0.10
- APRESSADO: custo 0.10, tempo 0.45, conforto 0.15, seguranca 0.20, eco 0.10
- CONFORTAVEL: custo 0.10, tempo 0.15, conforto 0.40, seguranca 0.25, eco 0.10
- ECOLOGICO: custo 0.15, tempo 0.10, conforto 0.10, seguranca 0.20, eco 0.45
- EQUILIBRADO: custo 0.20, tempo 0.20, conforto 0.20, seguranca 0.20, eco 0.20

## Modais Suportados

- CAR (carro proprio)
- RIDE_HAILING (Uber/99/InDrive)
- TAXI
- PUBLIC_TRANSIT_BUS (onibus)
- PUBLIC_TRANSIT_METRO (metro/trem)
- BICYCLE (bicicleta)
- WALKING (a pe)
- MOTORCYCLE (moto)

## Contextos Analisados

- Clima (chuva, calor extremo, tempestade)
- Transito (horario de pico, transito livre)
- Tipo de destino (bar, hospital, aeroporto, shopping)
- Horario (manha, tarde, noite, madrugada)
- Distancia (curta menor que 2km, longa maior que 20km)
- Padroes do usuario (aprendizado por contexto)

## Autoras

- Maira Louise Ferreira Silverio
- Milena Cristina da Silva
- Piettra Nobre Neves Santos

FATEC Praia Grande - Analise e Desenvolvimento de Sistemas - TCC 2026

## Licenca

MIT
