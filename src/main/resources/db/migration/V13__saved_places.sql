CREATE TABLE saved_places (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(300) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, address)
);
