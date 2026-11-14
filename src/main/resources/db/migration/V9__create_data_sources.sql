CREATE TABLE data_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    source_type VARCHAR(50) NOT NULL,
    base_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    failure_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
INSERT INTO data_sources (name, source_type, base_url, is_active) VALUES
('OpenWeatherMap', 'WEATHER', 'https://api.openweathermap.org/data/2.5', true),
('Google Places', 'PLACES', 'https://maps.googleapis.com/maps/api/place', false),
('OpenAI', 'AI', 'https://api.openai.com/v1', true);
