CREATE TABLE route_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    origin VARCHAR(300) NOT NULL,
    destination VARCHAR(300) NOT NULL,
    recommended_mode VARCHAR(30),
    recommended_label VARCHAR(60),
    price_formatted VARCHAR(30),
    duration_minutes INTEGER,
    bibi_message VARCHAR(600),
    card_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_route_history_user_date ON route_history (user_id, created_at DESC);
