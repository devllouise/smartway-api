CREATE TABLE context_patterns (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    destination_type VARCHAR(50) NOT NULL,
    time_of_day VARCHAR(30),
    day_type VARCHAR(20),
    most_chosen_mode VARCHAR(50),
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ctx_patterns_user ON context_patterns(user_id);
CREATE UNIQUE INDEX idx_ctx_patterns_unique ON context_patterns(user_id, destination_type, time_of_day, day_type);
