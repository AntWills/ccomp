CREATE TABLE tb_storage_files (
    file_name varchar(512) PRIMARY KEY,
    owner_user_id uuid NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_storage_files_owner_user_id
    ON tb_storage_files (owner_user_id);
