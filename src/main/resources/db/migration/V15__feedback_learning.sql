ALTER TABLE route_history ADD COLUMN IF NOT EXISTS destination_type VARCHAR(30);
ALTER TABLE route_history ADD COLUMN IF NOT EXISTS time_of_day VARCHAR(20);
ALTER TABLE route_history ADD COLUMN IF NOT EXISTS day_type VARCHAR(20);

CREATE TABLE route_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    history_id BIGINT NOT NULL UNIQUE REFERENCES route_history(id) ON DELETE CASCADE,
    helpful BOOLEAN,
    chosen_mode VARCHAR(30),
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_route_feedbacks_user ON route_feedbacks (user_id, created_at DESC);

CREATE TABLE weight_suggestions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    focus VARCHAR(20) NOT NULL,
    cost_weight DOUBLE PRECISION NOT NULL,
    time_weight DOUBLE PRECISION NOT NULL,
    comfort_weight DOUBLE PRECISION NOT NULL,
    safety_weight DOUBLE PRECISION NOT NULL,
    eco_weight DOUBLE PRECISION NOT NULL,
    reason VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);
CREATE INDEX idx_weight_suggestions_user ON weight_suggestions (user_id, status);
