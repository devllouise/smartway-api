INSERT INTO archetypes (name, description, default_cost_weight, default_time_weight, default_comfort_weight, default_safety_weight, default_eco_weight) VALUES
('EXECUTIVO', 'Chega rapido, sem abrir mao da seguranca', 0.10, 0.40, 0.15, 0.30, 0.05),
('PRUDENTE', 'Economiza sem correr riscos', 0.40, 0.10, 0.15, 0.30, 0.05),
('PRATICO', 'Prefere trajetos simples e diretos', 0.20, 0.25, 0.35, 0.15, 0.05),
('CONSCIENTE', 'Gasta pouco e polui pouco', 0.35, 0.10, 0.05, 0.15, 0.35),
('DINAMICO', 'Rapido, mas com consciencia ambiental', 0.15, 0.40, 0.05, 0.10, 0.30)
ON CONFLICT (name) DO NOTHING;
