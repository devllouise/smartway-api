CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    consultation_id BIGINT NOT NULL REFERENCES consultations(id),
    recommendation_helpful BOOLEAN,
    chosen_mode VARCHAR(50),
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_feedback_user ON feedbacks(user_id);
