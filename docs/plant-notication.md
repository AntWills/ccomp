# Plano: notificações de eventos

## Escopo

O subdomínio `events/notifications` permitirá à equipe enviar notificações aos inscritos do evento ou de uma atividade, consultar os envios e acompanhar a entrega. Inscritos poderão consultar as próprias notificações e marcar como lidas. Cada envio registra quem o iniciou e entrega tanto na plataforma quanto por email.

## Integração com módulos existentes

- Reutilizar `EmailService.send(SendMailDTO)` para envio de email e `EmailTemplateService` para renderizar o conteúdo. O envio deve ocorrer no consumer assíncrono, nunca durante a requisição HTTP.
- Declarar fila e dead-letter queue com `MessagingChannel`, conforme o padrão de RabbitMQ existente, e publicar mensagens JSON contendo identificadores de envio/destinatários, não listas grandes nem conteúdo binário.
- Consultar inscritos através das entidades atuais: `Enrollment` para inscrições no evento e `EnrollmentActivity` para inscrições na atividade. Considerar ativos os estados `CONFIRMED` e `CHECKED_IN`.
- Usar paginação por cursor com ordenação estável `createdAt DESC, id DESC`, seguindo os DTOs de cursor e `CursorUtils` usados em listagens existentes.

## Estrutura proposta

```text
events/notifications/
├── application/
│   ├── EventNotificationService
│   ├── EventNotificationQueryService
│   └── EventNotificationPublisher
├── persistence/
│   ├── EventNotification
│   ├── EventNotificationRecipient
│   ├── EventNotificationRepository
│   └── EventNotificationDslRepository
├── dto/
├── external/message/
│   ├── EventNotificationDispatchChannel
│   └── EventNotificationDeliveryChannel
├── listeners/
│   ├── EventNotificationDispatchConsumer
│   └── EventNotificationDeliveryConsumer
└── web/
    ├── EventNotificationManagementController
    └── MyEventNotificationsController
```

`EventNotification` guarda `eventId`, `activityId` opcional, `senderUserId`, título, mensagem, datas e estado geral. `EventNotificationRecipient` guarda `notificationId`, `userId`, email no momento do envio, estado/tentativas do email e `readAt`. Uma linha por destinatário permite acompanhar entrega e leitura individual sem duplicar o conteúdo.

## Regras de destinatários

- Envio para evento: inscritos ativos do evento.
- Envio para atividade: somente inscritos ativos associados àquela atividade. Não incluir automaticamente todos os inscritos do evento.
- Envio selecionado: validar que todos os usuários pertencem ao escopo de evento/atividade selecionado; remover IDs duplicados.

## Métodos e operações

```java
sendToEventSubscribers(eventId, request, senderUserId);
sendToActivitySubscribers(eventId, activityId, request, senderUserId);
sendToSelectedSubscribers(eventId, activityId, recipientIds, request, senderUserId);

listSentNotifications(eventId, cursor, limit);
getNotificationDeliveryStatus(eventId, notificationId);
listMyNotifications(cursor, limit);
markAsRead(notificationId);
markAllAsRead();
countUnreadNotifications();
```

A autorização de envio deve validar acesso de edição ao evento e, quando aplicável, pertencimento da atividade ao evento. Consultas e marcação de leitura devem usar o usuário autenticado.

## Processamento e lotes

1. A API cria o envio com remetente e conteúdo.
2. Um publicador pagina os destinatários por cursor e tamanho de lote configurável (por exemplo, 100), criando registros de destinatário pendentes.
3. Publica no RabbitMQ mensagens com os IDs daquele lote.
4. O consumer torna a notificação disponível na plataforma, envia email pelo módulo existente e atualiza tentativas/resultado.
5. Mensagens redeliveradas devem ser idempotentes para evitar notificações ou emails duplicados. Falhas transitórias podem ser repetidas; falhas definitivas ficam visíveis na DLQ e no estado persistido.

Para não perder trabalho entre a gravação no banco e a publicação RabbitMQ, as solicitações de distribuição e os lotes de entrega são gravados numa outbox na mesma transação. Um relay publica a outbox, aguarda confirmação do broker e tenta novamente em caso de falha. O cursor permite retomar o processamento sem carregar todos os inscritos na memória.

## Monitoramento

Expor por envio: quantidade total, pendentes, emails enviados e falhos; estado geral (`QUEUED`, `PROCESSING`, `COMPLETED`, `COMPLETED_WITH_ERRORS`); mensagens ainda pendentes na outbox, último erro de publicação e opção de reprocessar a distribuição ou destinatários falhos. O status deriva dos registros por destinatário e é atualizado transacionalmente ao concluir lotes.

## Endpoints sugeridos

- Equipe: enviar para evento, atividade ou seleção; listar envios do evento por cursor; consultar status de entrega.
- Inscrito: listar notificações pessoais por cursor, filtrar não lidas, marcar uma ou todas como lidas e consultar a contagem não lida.

## Decisões de implementação

- O lote de entrega é limitado a 100 destinatários e o envio selecionado a 500 IDs.
- A inscrição exige uma conta com email; esse endereço é copiado para o destinatário no momento do envio.
- A outbox é persistida no banco; o relay usa confirmação do RabbitMQ e continua tentando publicar pendências.
- Não inserir notificações diretamente em serviços de eventos/atividades: os pontos de negócio devem chamar o serviço de notificações ou publicar eventos internos, mantendo o subdomínio como dono do envio.
