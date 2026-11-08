# Fuso horário dos eventos

## Horários de agenda

Os campos de agenda sem offset — `start_date`, `end_date`, `enrollment_start_date` e `enrollment_end_date`, tanto de eventos quanto de atividades — representam horário de Brasília, com zona IANA `America/Sao_Paulo`. Envie valores locais sem offset, por exemplo `2026-11-08T09:05:00`. Não some nem subtraia horas no frontend.

As regras temporais do domínio de eventos recebem o `Clock` da aplicação. O bean usa `app.clock.zone`, configurado em `application.properties` com padrão `America/Sao_Paulo` e opção de override por `APP_CLOCK_ZONE`. Para manter o contrato dos horários de agenda, produção deve usar `America/Sao_Paulo`; o resultado dessas regras não depende do fuso padrão da JVM quando essa propriedade está configurada corretamente.

Para diagnóstico, `GET /api/server/status` retorna `timestamp` como instante UTC e também `server_local_time` e `time_zone`, gerados pelo mesmo `Clock` central. Em produção, o esperado é `time_zone: "America/Sao_Paulo"`; `server_local_time` deve ser interpretado nessa zona. A resposta também inclui o percentual do limite máximo de heap usado pela JVM. Isso permite comparar o horário efetivamente usado pelas regras com o relógio apresentado pelo frontend sem alterar o campo UTC já existente.

Essa referência é usada para calcular estados de execução e inscrição, publicar eventos, validar convites, registrar check-in/credenciamento e preencher datas de criação de materiais de evento. Aplica-se a eventos presenciais em qualquer estado e a eventos online.

## Metadados de auditoria

Nem todo campo `LocalDateTime` do backend é horário de agenda. Campos como `created_at` e `updated_at` de entidades que usam `@CreationTimestamp` e `@UpdateTimestamp` são gerados pelo Hibernate e não recebem diretamente o `Clock` central. O `app.clock.zone` não deve ser entendido como garantia de fuso para esses metadados nem para timestamps de outras áreas do sistema.

## Campo `time_zone` depreciado

O campo `time_zone` continua no schema e na API por compatibilidade. Está marcado como depreciado no OpenAPI. Na criação ele ainda é obrigatório pelo contrato atual; em atualizações permanece aceito; nas respostas continua sendo retornado. O backend ainda armazena o valor enviado, mas ele não determina a interpretação dos horários de agenda. Clientes devem deixar de usá-lo para conversão e informar `America/Sao_Paulo` enquanto o campo continuar obrigatório.

A migration V36 normaliza os valores já existentes para `America/Sao_Paulo` e define o mesmo valor como default. Ela não impede que novas chamadas de criação enviem outro valor para o campo legado.

## Borda de término

O instante de término é exclusivo para o estado de execução: no horário exatamente igual a `end_date`, o evento já é considerado encerrado. Assim, publicar às 09:10 um evento 09:05–12:00 é permitido, publicar às 12:00 é recusado e às 12:01 também é recusado. Um evento que começa e termina no mesmo dia segue as mesmas regras.

## Testes

Os testes de regra usam `Clock.fixed(...)`. A suíte focada foi executada com a JVM em UTC, incluindo o fluxo `PATCH /api/events/{id}/status/PUBLISHED`, para validar que a publicação não depende do timezone da máquina.
