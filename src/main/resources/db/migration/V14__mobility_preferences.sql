ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS multimodal VARCHAR(20);
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS transit_preference VARCHAR(20);
