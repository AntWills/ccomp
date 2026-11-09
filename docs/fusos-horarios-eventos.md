# Fuso horário global

## Horários de agenda

O projeto usa um único fuso global definido por `app.clock.zone`, com padrão `America/Sao_Paulo` e override por `APP_CLOCK_ZONE`. Todos os `LocalDateTime` representam horário local nessa zona, incluindo agenda, operações de domínio, metadados gerados pela aplicação e timestamps de persistência. Envie horários de agenda sem offset, por exemplo `2026-11-08T09:05:00`. Não some nem subtraia horas no frontend.

As regras temporais usam o `Clock` da aplicação. Na inicialização, o fuso padrão da JVM e o fuso da sessão PostgreSQL também são alinhados a `app.clock.zone`, para cobrir chamadas `LocalDateTime.now()` existentes e timestamps gerados pelo Hibernate ou pelo banco. Em produção, mantenha `America/Sao_Paulo` para preservar o contrato da API.

Para diagnóstico, `GET /api/server/status` retorna `timestamp` como instante UTC e também `server_local_time` e `time_zone`, gerados pelo mesmo `Clock` central. Em produção, o esperado é `time_zone: "America/Sao_Paulo"`; `server_local_time` deve ser interpretado nessa zona. A resposta também inclui o percentual do limite máximo de heap usado pela JVM. Isso permite comparar o horário efetivamente usado pelas regras com o relógio apresentado pelo frontend sem alterar o campo UTC já existente.

Essa referência é usada para calcular estados de execução e inscrição, publicar eventos, validar convites, registrar check-in/credenciamento e preencher datas de criação de materiais de evento. Aplica-se a eventos presenciais em qualquer estado e a eventos online.

## Campo `time_zone` depreciado

O campo `time_zone` continua no schema e nas respostas por compatibilidade, marcado como depreciado no OpenAPI. Não é aceito na criação nem em atualizações. O backend grava nele o fuso global e usa esse mesmo fuso para os horários. Clientes devem ignorar esse campo como configuração por evento; ele apenas informa o fuso global atual.

A migration V36 normaliza os valores existentes para `America/Sao_Paulo` e define o mesmo default. Novos eventos gravam o valor de `app.clock.zone` nesse campo legado.

## Borda de término

O instante de término é exclusivo para o estado de execução: no horário exatamente igual a `end_date`, o evento já é considerado encerrado. Assim, publicar às 09:10 um evento 09:05–12:00 é permitido, publicar às 12:00 é recusado e às 12:01 também é recusado. Um evento que começa e termina no mesmo dia segue as mesmas regras.

## Testes

Os testes de regra usam `Clock.fixed(...)`. A suíte focada foi executada com a JVM em UTC, incluindo o fluxo `PATCH /api/events/{id}/status/PUBLISHED`, para validar que a publicação não depende do timezone da máquina.
