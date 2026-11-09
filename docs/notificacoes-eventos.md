# Notificações de eventos: funcionamento

Este documento descreve o fluxo implementado no subdomínio `events/notifications`, suas funcionalidades e oportunidades de cache.

## Visão geral

Uma notificação tem um registro principal (`EventNotification`) com o conteúdo, evento, atividade opcional, remetente e estado geral. Cada inscrito alcançado tem um registro próprio (`EventNotificationRecipient`), que funciona como entrada da caixa na plataforma e acompanha o estado do email e a leitura. A `EventNotificationOutbox` guarda o trabalho que ainda precisa ser publicado no RabbitMQ.

O conteúdo da notificação fica no registro principal. As cópias por destinatário guardam os dados necessários para entrega e rastreio, incluindo nome e email capturados no momento do envio.

## Como acontece um envio

1. Um membro da equipe chama uma das rotas de envio. O serviço carrega o evento pelo `EventCache` e valida a permissão de edição. Para uma atividade, também verifica se ela pertence ao evento.
2. O serviço grava a notificação com estado `QUEUED` e persiste na outbox a solicitação de distribuição na mesma transação. A API responde `202 Accepted` com o ID e estado inicial.
3. `EventNotificationOutboxRelay` busca mensagens pendentes em ordem de ID, publica no RabbitMQ e aguarda confirmação. Se houver falha, registra tentativas e erro para uma nova tentativa posterior.
4. O consumer de distribuição chama `EventNotificationDispatchProcessor`. Ele percorre os inscritos em páginas por cursor, em lotes de 100. O QueryDSL consulta inscrições ativas (`CONFIRMED` e `CHECKED_IN`) e usa ordenação estável para continuar a partir do cursor.
5. Para cada inscrito, o processador cria ou reutiliza o registro de destinatário. Isso torna a retomada idempotente: um destinatário já criado não é duplicado. Os IDs são encaminhados para entrega em lotes através da outbox.
6. O consumer de entrega chama `EventNotificationDeliveryProcessor`. O destinatário é reivindicado atomicamente como pendente, recebe o email por `EventNotificationEmailService` e tem o resultado persistido. O registro já serve como notificação na plataforma; `readAt` começa vazio.
7. Ao terminar a distribuição, o estado geral é recalculado com base nos destinatários. Emails pendentes mantêm o envio em `PROCESSING`; sem pendências, o resultado é `COMPLETED` ou `COMPLETED_WITH_ERRORS`.

O email específico e o carregamento/renderização do template pertencem a `notifications/email`. O módulo compartilhado `EmailService` é usado apenas como transporte. Templates ficam em `resources/templates/email` e usam o layout comum.

## Público e seleção de destinatários

- **Evento:** inclui apenas inscrições ativas no evento.
- **Atividade:** inclui apenas inscrições ativas vinculadas àquela atividade. Quem está inscrito somente no evento não é adicionado automaticamente.
- **Seleção manual:** aceita até 500 IDs de usuário, remove IDs repetidos e valida que todos pertencem ao escopo de evento ou atividade informado. Se algum ID não estiver inscrito nesse escopo, a solicitação é rejeitada.

## Funcionalidades disponíveis

### Equipe

- Enviar uma notificação para inscritos de evento, atividade ou lista selecionada.
- Consultar o histórico de envios de um evento por cursor, incluindo remetente, conteúdo, audiência e estado.
- Consultar progresso e resultado: total de destinatários, emails pendentes, enviados e falhos, mensagens pendentes na outbox e último erro de publicação.
- Reenfileirar a distribuição se ela ainda não terminou.
- Reenfileirar emails que falharam sem criar novamente as entradas da caixa na plataforma.

Essas operações exigem permissão de edição do evento.

### Inscrito

- Listar a própria caixa de notificações por cursor e filtrar as não lidas.
- Consultar a contagem de notificações não lidas.
- Marcar uma notificação como lida ou marcar todas como lidas.

O inscrito só pode alterar o registro de destinatário associado à própria conta. O `recipientId` usado na rota de leitura é o ID dessa associação, retornado como `id` na listagem pessoal.

As listagens têm ordenação estável decrescente por data e ID e aceitam páginas de até 50 itens. Os endpoints estão documentados no Swagger.

## Entidades persistidas

| Entidade | Utilidade |
| --- | --- |
| `EventNotification` | Conteúdo e estado agregado de um envio, com evento, atividade opcional e remetente. |
| `EventNotificationRecipient` | Caixa e estado individual de cada inscrito, incluindo leitura e entrega de email. |
| `EventNotificationOutbox` | Publicação confiável de trabalho assíncrono no RabbitMQ, com tentativas, confirmação e erro. |

## Cache: implementação e cuidados

- **Templates de email:** o layout e o fragmento são carregados uma vez quando `EventNotificationEmailService` inicia e reutilizados em memória. Como são recursos imutáveis no classpath, não precisam de invalidação ou Redis.
- **Contagem de não lidas:** `GET /unread-count` usa Redis por usuário. O cache é invalidado quando o inscrito marca uma ou todas como lidas e quando a distribuição cria destinatários. O TTL volátil serve como proteção adicional.
- **Histórico da equipe:** as páginas de envios são cacheadas por evento, cursor e tamanho da página. O cache é limpo ao criar/reprocessar envios e quando o processamento assíncrono atualiza destinatários ou status.
- **Imagem de capa do evento:** os bytes são cacheados pela chave imutável do objeto, seguindo o padrão usado nas imagens de convidados. Substituir ou remover a capa invalida a imagem antiga; falha no Redis recua para o storage.

A caixa pessoal, o estado detalhado de entrega e as listas de destinatários continuam sem cache, pois mudam com frequência ou precisam refletir imediatamente leitura, email e progresso. Os destinatários seguem sendo lidos em lotes por cursor. Os caches Redis usam os TTLs definidos em `app.cache.tiers` e podem ser ajustados sem mudar as rotas.

## Rotas

### Gestão — `/api/events/{eventId}/notifications`

| Método e rota | Ação |
| --- | --- |
| `POST /` | Envia aos inscritos do evento. |
| `POST /activities/{activityId}` | Envia aos inscritos da atividade. |
| `POST /selected` | Envia aos IDs selecionados e validados. |
| `GET /` | Lista o histórico de envios do evento. |
| `GET /{notificationId}/delivery` | Consulta andamento e erros. |
| `POST /{notificationId}/retry-failed` | Reenfileira emails falhos. |
| `POST /{notificationId}/retry-dispatch` | Retoma uma distribuição incompleta. |

### Inscrito — `/api/events/me/notifications`

| Método e rota | Ação |
| --- | --- |
| `GET /` | Lista a caixa pessoal; aceita `unreadOnly`, `nextCursor` e `pageSize`. |
| `GET /unread-count` | Retorna a contagem de não lidas. |
| `PUT /{recipientId}/read` | Marca uma entrada da caixa como lida. |
| `PUT /read-all` | Marca todas as entradas da caixa como lidas. |
