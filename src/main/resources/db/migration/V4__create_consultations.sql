CREATE TABLE consultations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    origin_address VARCHAR(500) NOT NULL,
    origin_lat DOUBLE PRECISION NOT NULL,
    origin_lng DOUBLE PRECISION NOT NULL,
    destination_address VARCHAR(500) NOT NULL,
    destination_lat DOUBLE PRECISION NOT NULL,
    destination_lng DOUBLE PRECISION NOT NULL,
    destination_type VARCHAR(50),
    trip_intent VARCHAR(50),
    weather_condition VARCHAR(100),
    weather_temp DOUBLE PRECISION,
    weather_rain_probability DOUBLE PRECISION,
    traffic_level VARCHAR(50),
    traffic_delay_minutes INTEGER,
    context_insights JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_consult_user ON consultations(user_id);
