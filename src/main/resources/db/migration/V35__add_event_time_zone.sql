-- Legacy timestamps were entered as local wall-clock values. Keep their current
-- interpretation explicit; new events provide their own IANA time zone.
alter table tb_events
    add column time_zone varchar(64) not null default 'America/Sao_Paulo';
