ALTER TABLE tb_refresh_token
    ADD COLUMN family_id UUID NOT NULL,
    ADD COLUMN revoked BOOL NOT NULL,
    ADD COLUMN created_at TIMESTAMP NOT NULL;

create index idx_refresh_token_family_id on tb_refresh_token (family_id);