CREATE TABLE transport_options (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
    mode VARCHAR(50) NOT NULL,
    estimated_cost NUMERIC(10,2) NOT NULL,
    estimated_time_minutes INTEGER NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL,
    comfort_score DOUBLE PRECISION NOT NULL,
    safety_score DOUBLE PRECISION NOT NULL,
    eco_score DOUBLE PRECISION NOT NULL,
    final_score DOUBLE PRECISION NOT NULL,
    ranking INTEGER NOT NULL,
    description VARCHAR(500),
    cost_breakdown JSONB,
    context_penalized BOOLEAN DEFAULT FALSE,
    context_boosted BOOLEAN DEFAULT FALSE
);
