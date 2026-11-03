-- Convidados são perfis públicos do evento e podem existir sem conta.
ALTER TABLE tb_event_guests ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE tb_event_guests ADD COLUMN name VARCHAR(255);
ALTER TABLE tb_event_guests ADD COLUMN description TEXT;
ALTER TABLE tb_event_guests ADD COLUMN image_url VARCHAR(2048);

-- Preserva registros existentes ao introduzir o nome obrigatório.
UPDATE tb_event_guests eg
SET name = COALESCE(NULLIF(u."name", ''), 'Convidado')
FROM tb_users u
WHERE eg.user_id = u.id;

UPDATE tb_event_guests SET name = 'Convidado' WHERE name IS NULL;
ALTER TABLE tb_event_guests ALTER COLUMN name SET NOT NULL;

CREATE INDEX idx_event_guests_event_name ON tb_event_guests(event_id, name);
