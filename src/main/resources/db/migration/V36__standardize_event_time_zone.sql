-- Event LocalDateTime values are interpreted as Brasilia time by the backend.
-- Keep the compatibility field and its stored values aligned with that default.
update tb_events
set time_zone = 'America/Sao_Paulo'
where time_zone is distinct from 'America/Sao_Paulo';

alter table tb_events
    alter column time_zone set default 'America/Sao_Paulo';
