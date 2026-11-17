ALTER TABLE tb_news RENAME COLUMN cover_image_url TO cover_image_key;

-- Valores antigos apontam para URLs externas e não são chaves do storage.
UPDATE tb_news SET cover_image_key = NULL WHERE cover_image_key IS NOT NULL;
