TRUNCATE TABLE tb_refresh_token;

ALTER TABLE tb_refresh_token
    DROP COLUMN token,
    ADD COLUMN token_hash VARCHAR(64) NOT NULL;

ALTER TABLE tb_refresh_token
    ADD CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash);

CREATE INDEX idx_refresh_token_user_id ON tb_refresh_token (user_id);
CREATE INDEX idx_refresh_token_expiry_date ON tb_refresh_token (expiry_date);