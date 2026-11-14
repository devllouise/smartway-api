CREATE TABLE archetypes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    default_cost_weight DOUBLE PRECISION NOT NULL DEFAULT 0.35,
    default_time_weight DOUBLE PRECISION NOT NULL DEFAULT 0.30,
    default_comfort_weight DOUBLE PRECISION NOT NULL DEFAULT 0.20,
    default_safety_weight DOUBLE PRECISION NOT NULL DEFAULT 0.10,
    default_eco_weight DOUBLE PRECISION NOT NULL DEFAULT 0.05
);
INSERT INTO archetypes (name, description, default_cost_weight, default_time_weight, default_comfort_weight, default_safety_weight, default_eco_weight) VALUES
('ECONOMICO', 'Prioriza economia', 0.50, 0.20, 0.10, 0.10, 0.10),
('APRESSADO', 'Prioriza velocidade', 0.15, 0.50, 0.20, 0.10, 0.05),
('CONFORTAVEL', 'Prioriza conforto', 0.15, 0.20, 0.45, 0.15, 0.05),
('EQUILIBRADO', 'Balanco geral', 0.30, 0.30, 0.20, 0.10, 0.10),
('SUSTENTAVEL', 'Prioriza ecologia', 0.20, 0.15, 0.10, 0.10, 0.45);
