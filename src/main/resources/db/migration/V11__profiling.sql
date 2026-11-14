ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS available_modes VARCHAR(200);
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS priorities VARCHAR(100);

INSERT INTO archetypes (name, description, default_cost_weight, default_time_weight, default_comfort_weight, default_safety_weight, default_eco_weight) VALUES
('SABIO', 'Equilibra rapidez e economia', 0.40, 0.40, 0.08, 0.07, 0.05),
('ATIVO', 'Prefere se movimentar', 0.20, 0.15, 0.05, 0.15, 0.45),
('CUIDADOSO', 'Prioriza seguranca e conforto', 0.15, 0.15, 0.30, 0.35, 0.05)
ON CONFLICT (name) DO NOTHING;
