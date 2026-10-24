CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_users_name_trgm
    ON tb_users USING gin (name gin_trgm_ops);

CREATE INDEX idx_users_email_address_trgm
    ON tb_users USING gin (email_address gin_trgm_ops);
