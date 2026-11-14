-- Remove estruturas da primeira versao que nao sao mais usadas
DROP TABLE IF EXISTS feedbacks;
DROP TABLE IF EXISTS recommendations;
DROP TABLE IF EXISTS transport_options;
DROP TABLE IF EXISTS consultations;
DROP TABLE IF EXISTS context_patterns;
DROP TABLE IF EXISTS data_sources;

-- Arquetipos que nao sao mais gerados pela triagem
UPDATE user_preferences SET archetype_id = (SELECT id FROM archetypes WHERE name = 'SUSTENTAVEL')
 WHERE archetype_id IN (SELECT id FROM archetypes WHERE name IN ('ATIVO', 'CONFORTAVEL'));
DELETE FROM archetypes WHERE name IN ('ATIVO', 'CONFORTAVEL');
