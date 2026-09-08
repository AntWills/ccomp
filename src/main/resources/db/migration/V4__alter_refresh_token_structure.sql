ALTER TABLE tb_refresh_token
    ADD COLUMN user_agent VARCHAR(500),
    ADD COLUMN ip_address VARCHAR(45);

ALTER TABLE tb_refresh_token
    DROP CONSTRAINT tb_refresh_token_user_id_key;

ALTER TABLE tb_refresh_token
    ALTER COLUMN token TYPE UUID
    USING token::uuid;