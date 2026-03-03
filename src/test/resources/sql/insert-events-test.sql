INSERT INTO tb_events (id, name, start_date, end_date, owner_id)
VALUES
(1, 'Evento Teste Fechado', NULL, NULL, '18b31f15-e2e8-4a8a-a1fc-4dc17d131ef1'),
(2, 'Evento Teste Aberto', '2026-02-02 00:00:00', '2026-03-15 00:00:00', '18b31f15-e2e8-4a8a-a1fc-4dc17d131ef1'),
(3, 'Evento Teste Fechado - 02', NULL, NULL, '3af60e1a-ce9d-45a3-0a26-ad1e2f3a4b10'),
(4, 'Evento Teste Aberto - 02', '2026-02-02 00:00:00', '2026-03-15 00:00:00', '3af60e1a-ce9d-45a3-0a26-ad1e2f3a4b10');

-- O owner está vindo da tabela de users.