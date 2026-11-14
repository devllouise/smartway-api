CREATE TABLE recommendations (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL UNIQUE REFERENCES consultations(id) ON DELETE CASCADE,
    recommended_mode VARCHAR(50) NOT NULL,
    headline VARCHAR(500) NOT NULL,
    explanation TEXT NOT NULL,
    reasons JSONB NOT NULL,
    alternatives JSONB NOT NULL,
    context_insights JSONB,
    weights_used JSONB NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
