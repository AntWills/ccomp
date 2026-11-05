# Modelo inicial de credenciamento de eventos

**Status:** modelo inicial implementado no backend; impressão visual/PDF, walk-in e sincronização offline continuam planejados.

## 1. Objetivo e decisões de domínio

O credenciamento registra a chegada do inscrito ao evento e organiza a operação de recepção: validação da inscrição, estação, operador e entrega/reimpressão de crachá. Ele não substitui a presença em atividades, que continua no subdomínio de atividades.

Há uma correção importante em relação ao diagnóstico anterior: `Enrollment` já representa a chegada ao evento. Seu estado `CHECKED_IN` e o campo `checkInAt` são usados para presença no evento e emissão do certificado. O `CheckInService` de atividades registra outra coisa. Portanto:

- `Enrollment.status` e `Enrollment.checkInAt` continuam sendo a fonte oficial de presença no evento;
- `EventCredential` guarda os dados operacionais do atendimento e do crachá, sem criar uma segunda presença concorrente;
- ao credenciar com sucesso, a mesma transação chama `Enrollment.checkIn()` e cria/atualiza `EventCredential`;
- reimpressão não altera presença; revogar uma credencial ou QR também não apaga a chegada;
- desfazer presença é uma correção administrativa explícita, auditada e separada de revogar/reemitir crachá.

`Enrollment` aceita hoje `CONFIRMED`, `CHECKED_IN`, `CANCELED` e `BANNED`. Não existe estado de pagamento no modelo consultado; logo, o MVP não afirma que uma inscrição está paga. Se houver cobrança externa, a recepção encaminha a pendência a um guichê responsável, sem credenciar como paga nem alterar a inscrição. Uma integração futura deverá definir uma fonte de pagamento verificável.

## 2. Modelo de dados

### `EventCredential`

Um registro por `(event_id, enrollment_id)`, com constraint única. A linha existe depois do primeiro credenciamento e representa o atendimento operacional, não a presença em si.

Campos propostos:

| Campo | Regra |
|---|---|
| `id` | Identificador interno. |
| `event_id`, `enrollment_id` | Obrigatórios; a inscrição deve pertencer ao evento indicado. Constraint única composta. |
| `credentialed_at`, `credentialed_by` | Data/hora no mesmo padrão local usado pelo backend e usuário autenticado que concluiu o primeiro atendimento; imutáveis em reescaneamento. |
| `station` | Identificador curto da estação informado pelo cliente autenticado; sem texto livre com dados pessoais. |
| `badge_print_count`, `last_badge_printed_at` | Contador e última impressão, atualizados apenas pela operação de impressão/reimpressão autorizada. A emissão inicial pode contar como 1. |
| `created_at`, `updated_at` | Auditoria técnica da linha. |

Não armazenar nome, e-mail, CPF, cópia do QR nem uma segunda coluna `status` nesta tabela. A elegibilidade deriva de `Enrollment.status`; a revogação do QR fica no token próprio. O histórico detalhado de correções, revogações e reimpressões vai para auditoria, sem copiar mais dados pessoais do que o necessário.

Migration proposta: `V28__create_event_credentials.sql`. Incluir FKs, `NOT NULL`, índice/constraint único `(event_id, enrollment_id)`, índice de consulta `(event_id, credentialed_at, id)` e constraints para contagem não negativa. Para garantir no banco que a inscrição pertence ao mesmo evento, incluir uma chave única referenciável em `tb_event_enrollments(events_id, id)` e uma FK composta de `tb_event_credentials(event_id, enrollment_id)` para ela; a aplicação também valida essa associação.

### Token QR

Não reutilizar access token ou refresh token de autenticação. Emitir token aleatório opaco, com entropia criptográfica suficiente, vinculado a uma inscrição e a um evento. Persistir somente hash com algoritmo apropriado para tokens de alta entropia. O token não contém nome, e-mail, CPF ou outros dados legíveis.

O QR é válido até ser revogado/rotacionado. Reescaneamento é permitido e idempotente: retorna o resultado existente e não duplica presença, credencial ou auditoria de primeiro credenciamento. Revogar/rotacionar o token invalida o QR antigo, sem remover a presença já registrada. A emissão inicial deve ser autenticada e limitada à própria inscrição; um editor não deve conseguir obter o QR de todos os participantes por uma rota pública.

## 3. Elegibilidade e transições

| Estado da inscrição | Resultado do scan/credenciamento | Ação da recepção |
|---|---|---|
| `CONFIRMED` | Elegível; cria `EventCredential` e marca chegada atomicamente. | Entregar crachá e orientar participante. |
| `CHECKED_IN` e já existe `EventCredential` | Sucesso idempotente: “já credenciado”, sem nova presença ou impressão implícita. | Conferir se precisa de reimpressão. |
| `CHECKED_IN` sem `EventCredential` (dados legados ou inconsistentes) | Não marcar chegada novamente; registrar reconciliação auditada, se a identidade for validada. | Entregar crachá após confirmação; operação retorna indicação de registro reconciliado. |
| `CANCELED` | Recusado, sem alterar inscrição. | Direcionar ao responsável por inscrições. Reativação segue o fluxo existente de inscrição, nunca o scanner. |
| `BANNED` | Recusado, sem revelar detalhes além de “inscrição não elegível”. | Encaminhar discretamente ao responsável designado; não discutir o motivo na fila. |
| Inscrição/evento inexistente ou QR inválido/revogado | Recusado sem alteração. | Busca manual controlada ou atendimento de exceção. |

Não existe `PENDING` em `EnumEnrollmentState` atualmente. Se surgir aprovação ou espera, definir o estado e a política antes de aceitar esse caso no credenciamento. A rota de credenciamento nunca confirma, reativa ou desbloqueia uma inscrição implicitamente.

### Corrigir ou desfazer

`DELETE` na credencial não significa desfazer a chegada. O fluxo de correção deve exigir permissão de supervisor, motivo obrigatório e registrar estado anterior/novo, operador, horário e evento. A correção sincroniza `Enrollment.status` e `checkInAt` segundo a regra de domínio e registra evento de auditoria; ela deve ser recusada se houver dependência já emitida (por exemplo, certificado), salvo uma rotina explícita de invalidação desses artefatos. Até essa rotina existir, não oferecer “desfazer presença” no MVP. Revogar o QR, corrigir chegada e reimprimir crachá são ações distintas.

## 4. Permissões e privacidade

O projeto não tinha um enum de permissão por editor. O MVP implementa a permissão operacional como concessão por evento na tabela `tb_event_credential_operators`: `PUT/DELETE /operators/{userId}` permite ao proprietário, editor ativo, `MODERATOR` ou `ADMIN` conceder/remover operador. Um operador ativo pode buscar, escanear, credenciar, imprimir e consultar o resumo, sem ganhar edição geral. Conta do operador precisa estar ativa. Não presumir que o cargo global `STAFF`, sozinho, dá acesso ao credenciamento de qualquer evento.

As rotas de staff exigem JWT e autorização no serviço, além das regras HTTP. Busca por nome/e-mail/CPF retorna campos mínimos: ID da inscrição, nome para conferência, estado e indicação de elegibilidade/credenciamento. CPF não deve ser devolvido nem aparecer em logs; se for usado como critério, aplicar mascaramento na interface e limitar resultados. A busca textual pode reaproveitar infraestrutura `pg_trgm`/DSL, mas deve escolher campos e limiar próprios, limitar tamanho e quantidade de resultados e não ampliar o endpoint administrativo de usuários.

Auditar, no mínimo: credenciamento inicial, reconciliação, tentativa administrativa de correção, correção executada, revogação/rotação de token e impressão/reimpressão. Registrar ator, evento/inscrição, timestamp, estação e motivo quando aplicável; nunca token bruto, QR ou CPF. Usar `AuditService`/`EnumActionType` conforme o formato existente. A trilha de auditoria é restrita a quem já pode consultar auditoria administrativa.

## 5. API inicial

Prefixo implementado: `/api/events/{eventId}/credentials`. Todas as rotas exigem JWT e autorização no domínio.

| Método e rota | Uso | Permissão |
|---|---|---|
| `POST /scan` | Validar token e credenciar; corpo `{ "token": "...", "station": "A" }`. | `CREDENTIALING` ou gestão do evento. |
| `POST /manual/search` | Buscar por termo e devolver resultados mínimos; corpo com `searchTerm`, máximo 20 resultados. | Operador ou gestão do evento. |
| `POST /manual` | Credenciar inscrição selecionada; corpo com `enrollmentId` e `station`. | Operador ou gestão do evento. |
| `POST /{credentialId}/reprint` | Registrar impressão/reimpressão; corpo `{ "station": "A" }`. Cada chamada incrementa o contador. | Operador ou gestão do evento. |
| `GET /summary` | Totais de inscrições elegíveis, credenciadas e restantes. | Operador ou gestão do evento. |
| `POST /my-token` | Emitir o token pessoal uma vez; o token bruto só é retornado nesta emissão. | Participante autenticado com inscrição elegível. |
| `POST /my-token/rotate` | Invalidar o token anterior e devolver um token novo. | Participante autenticado para si. |
| `POST /enrollments/{enrollmentId}/token/rotate` | Revogar o QR atual e emitir outro. | `CREDENTIALING` ou gestão do evento. |
| `PUT /operators/{userId}` / `DELETE /operators/{operatorUserId}` | Conceder/remover o operador temporário do evento. | Gestão do evento. |

O corpo de busca manual é `{ "searchTerm": "..." }`; a resposta limita a 20 inscrições e mascara o e-mail. O credenciamento manual recebe `enrollmentId` e `station`. As respostas de scan/manual incluem `credentialId`, estado, resultado e dados mínimos do participante. Use `credentialId` na operação de impressão/reimpressão; a API ainda não gera o PDF do crachá.

Não criar `DELETE /credentials/{id}` no MVP. Correção de presença terá rota/contrato próprio depois de definida a invalidação de certificados. Walk-in também fica fora do MVP porque requer criação/reativação de usuário e inscrição e não deve ser improvisado no scanner.

### Respostas e erros

Resposta de sucesso inclui `result` (`CREDENTIALED`, `ALREADY_CREDENTIALED` ou `RECONCILED`), `enrollmentId`, estado de elegibilidade, `credentialedAt` e indicação se deve entregar/imprimir crachá. Não retornar segredo do QR.

Tratar os erros com códigos estáveis no corpo de erro, seguindo o envelope já usado pelo projeto:

| HTTP | Código sugerido | Conduta no cliente |
|---|---|---|
| `400` | `INVALID_CREDENTIAL_TOKEN` | Informar QR inválido/expirado; oferecer busca manual. |
| `403` | `CREDENTIALING_FORBIDDEN` | Encaminhar ao supervisor; não repetir automaticamente. |
| `404` | `ENROLLMENT_NOT_FOUND` | Confirmar evento e tentar busca manual. |
| `409` | Inscrição não elegível ou QR inválido/revogado (mensagem do envelope atual, sem código estável específico). | Mostrar mensagem segura e encaminhar ao fluxo de exceção. |
| `409` ou resposta idempotente | `ALREADY_CREDENTIALED` | Não bloquear a fila; oferecer somente reimpressão se solicitada. |
| `429` | `CREDENTIALING_RATE_LIMITED` | Aguardar ou usar a busca manual autorizada. |
| `5xx`/indisponibilidade | `CREDENTIALING_UNAVAILABLE` | Acionar procedimento de contingência e anotar pendência para reconciliação. |

Scan e credenciamento manual são idempotentes por inscrição: repetição ou concorrência retorna `ALREADY_CREDENTIALED`, apoiada por bloqueio da linha de inscrição e constraint única `(event_id, enrollment_id)`. A primeira estação/operador/horário são preservados. Reimpressão é uma ação separada e cada chamada incrementa o contador; o cliente não deve repetir automaticamente essa chamada após timeout sem antes consultar o estado da operação.

## 6. Procedimento operacional no primeiro dia

### Preparação

1. Antes de abrir, atualizar a lista autorizada a partir das inscrições, sem exportar CPF para planilhas de operação.
2. Confirmar evento/data, operadores e permissões `CREDENTIALING`; cada estação usa identificador curto e único.
3. Testar leitor, rede, impressora, layout e QR; separar crachás em branco e etiquetas para correções de nome.
4. Preparar sinalização e filas: QR/inscritos; ajustes e busca manual; exceções (inscrição cancelada/bloqueada, pagamento externo ou divergência).
5. Definir um supervisor e registrar um meio de contato interno. Pagamento pendente vai ao guichê designado, sem parar a fila principal.

### Atendimento

1. Ler QR; em falha, procurar por nome ou e-mail. CPF só é usado se necessário, em campo protegido, sem anotação em papel.
2. Conferir visualmente o nome e a elegibilidade. Para homônimos ou divergência, confirmar segundo dado mínimo (por exemplo, e-mail parcialmente mascarado) e chamar supervisor; não mostrar dados de outra pessoa.
3. Confirmar credenciamento uma vez, entregar crachá e orientar sobre programação e espaços. Se já credenciado, não criar outra presença; perguntar se precisa de reimpressão.
4. Inscrição cancelada/bloqueada, pagamento externo pendente ou QR sem correspondência seguem para fila de exceções. Atendente não altera status nem promete aprovação.
5. Reimpressão registra operador, estação e motivo opcional. Recolher/inutilizar crachá anterior quando viável, sem condicionar a validade da presença.

### Fechamento

Conferir contagem de credenciados, falhas e exceções; garantir que nenhuma correção ficou pendente; guardar materiais com dados pessoais em local restrito e destruir impressos temporários conforme política da organização. Revogar acessos temporários da equipe após o evento.

## 7. Contingência, offline e sincronização

O MVP é online e não aceita scans offline como credenciamento confirmado. Se API/rede indisponível: direcionar a uma fila de contingência, verificar lista previamente preparada com dados mínimos, anotar apenas nome, identificador da inscrição se disponível, estação, hora e operador em formulário controlado, e marcar a pessoa como “pendente de sincronização”, sem afirmar que a presença foi registrada no sistema. Um supervisor revisa e importa/reconcilia quando o serviço voltar, evitando duplicar registros existentes.

Não guardar tokens QR em cache local ou logs. Modo offline futuro exige dispositivo autenticado, lista criptografada e com validade curta, armazenamento protegido, revogação/expiração, deduplicação local e servidor, trilha de operador e resolução explícita de conflito. Até esses requisitos serem implementados e avaliados, manter o modo offline fora do contrato da API.

## 8. Painel, métricas e exportação

O resumo deve derivar contagens de `Enrollment` e `EventCredential`, e não de contadores de cache como fonte de verdade. Métricas por hora/estação podem ser agregadas a partir dos horários e eventos auditados. Exportação CSV não faz parte do MVP; quando implementada, precisa de autorização de gestão, campos mínimos, registro da exportação e prazo de retenção. Cache pode acelerar a leitura, mas deve ser invalidado na mesma operação lógica e ser reconstruível do banco.

## 9. Fases

| Fase | Entregas |
|---|---|
| **MVP** | Implementado: migration e persistência operacional; integração transacional com `Enrollment.checkIn()`; QR opaco com hash, scan, busca e credenciamento manual; operadores temporários por evento; idempotência/concorrência em scan/manual; auditoria; contador de impressão/reimpressão; resumo operacional. Pendente: códigos de erro estáveis específicos, idempotência por chave para reimpressão e gerador/template PDF do crachá. |
| **Fase 2** | Walk-in com fluxo explícito de usuário/inscrição; template e impressão em lote de crachás; política de exigência de credenciamento no check-in de atividades, somente se necessária e sem quebrar certificados/fluxos existentes; exportação controlada. |
| **Fase 3** | Modo offline com pacote criptografado e validade limitada, sincronização e resolução de conflitos; múltiplas estações com métricas detalhadas e fila/retentativa. |

## 10. Critérios de aceite do modelo

- Uma inscrição `CONFIRMED` gera no máximo uma chegada e uma credencial, mesmo com scans simultâneos ou repetidos.
- `CANCELED` e `BANNED` não podem ser credenciadas nem reativadas pela API de recepção.
- Inscrição `CHECKED_IN` legada pode ser reconciliada sem alterar sua data de presença nem duplicar chegada.
- Reimprimir ou revogar QR não altera `Enrollment.status`/`checkInAt`.
- Token QR não é JWT de login, não contém PII e só o hash é persistido.
- Atendente com `CREDENTIALING` não ganha permissão geral de edição; falta de permissão e erros de estado têm resposta tratável pela interface.
- A auditoria permite identificar operador, evento, inscrição, estação e operação sem registrar segredo ou CPF.
- Nenhuma tela ou relatório indica pagamento confirmado sem integração com uma fonte de pagamento.
- Falha de rede não é apresentada como credenciamento concluído; contingência offline é reconciliável e não cria duplicatas.

## Referências de implementação existentes

- `Enrollment`, `EnumEnrollmentState` e `EnrollmentsServices`: inscrição e presença no evento.
- `CheckInService`, `CheckIn` e `EnrollmentActivity`: presença em atividades.
- `EventAccessPolicy` e `EventEditorPermission`: autorização e editores.
- `JwtService`: tokens de autenticação; não usar seu access/refresh token como QR de credenciamento.
- `AuditService`: consultas de auditoria existentes; definir os novos tipos de ação no mesmo padrão.
- `TokenHasher`: hashing de tokens de alta entropia, se validado como adequado ao token de credenciamento.
- `V22__add_user_text_search.sql`: infraestrutura de busca textual, não uma regra de relevância obrigatória para credenciamento.
