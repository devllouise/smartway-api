CREATE TABLE preference_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    cost_weight DOUBLE PRECISION NOT NULL,
    time_weight DOUBLE PRECISION NOT NULL,
    comfort_weight DOUBLE PRECISION NOT NULL,
    safety_weight DOUBLE PRECISION NOT NULL,
    eco_weight DOUBLE PRECISION NOT NULL,
    trigger_reason VARCHAR(200) NOT NULL,
    version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
