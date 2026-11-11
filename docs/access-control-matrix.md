# Matriz de acesso às rotas

Gerada a partir dos controllers e do `SecurityConfig`. Rotas não listadas por anotação específica ficam protegidas pelo JWT global. A verificação de autoria e propriedade ocorre nos serviços; endpoints públicos de busca só retornam dados publicados.

Para o funcionamento completo, os vínculos entre destinatários, elegibilidade, templates, variáveis e validação dos certificados, consulte [Certificados de eventos](event-certificado.md).

| Método | Rota | Controller#método | Nível | Regra adicional |
|---|---|---|---|---|
| `GET` | `/actuator/health` | `SecurityConfig#health` | Pública | Healthcheck sem detalhes em produção. |
| `GET` | `/actuator/info` | `SecurityConfig#info` | Pública | Informações da aplicação. |
| `POST` | `/api/admin/audit-logs/users/search` | `AuditLogController#getAuditLogs` | Restrita por role | Roles: ADMIN, MODERATOR. Políticas de propriedade também se aplicam. |
| `GET` | `/api/admin/users/email/{email}` | `UserAdminController#getByEmail` | Restrita por role | Roles: ADMIN, MODERATOR. Políticas de propriedade também se aplicam. |
| `POST` | `/api/admin/users/search` | `UserAdminController#searchUsers` | Restrita por role | Roles: ADMIN, MODERATOR. Políticas de propriedade também se aplicam. |
| `GET` | `/api/admin/users/{userId}` | `UserAdminController#getById` | Restrita por role | Roles: ADMIN, MODERATOR. Políticas de propriedade também se aplicam. |
| `PATCH` | `/api/admin/users/{userId}/block` | `UserAdminController#blockByEmail` | Restrita por role | Roles: ADMIN, MODERATOR. Políticas de propriedade também se aplicam. |
| `PUT` | `/api/admin/users/{userId}/roles/{role}` | `UserAdminController#changeRole` | Restrita por role | Exclusiva de ADMIN (gestão de cargos). |
| `PATCH` | `/api/admin/users/{userId}/unlock` | `UserAdminController#unlockByEmail` | Restrita por role | Roles: ADMIN, MODERATOR. Políticas de propriedade também se aplicam. |
| `POST` | `/api/auth/forgot-password` | `AuthController#forgotPassword` | Pública | Pública; login e recuperação usam respostas genéricas. |
| `POST` | `/api/auth/logout` | `AuthController#logout` | Pública | Pública; login e recuperação usam respostas genéricas. |
| `POST` | `/api/auth/refresh` | `AuthController#refresh` | Pública | Pública; login e recuperação usam respostas genéricas. |
| `POST` | `/api/auth/reset-password` | `AuthController#resetPassword` | Pública | Pública; login e recuperação usam respostas genéricas. |
| `POST` | `/api/auth/sign-in` | `AuthController#login` | Pública | Pública; login e recuperação usam respostas genéricas. |
| `POST` | `/api/auth/sign-up` | `AuthController#signUp` | Pública | Pública; login e recuperação usam respostas genéricas. |
| `GET` | `/api/events/certificates/validate/{code}` | `EventCertificateValidationController#validateCertificate` | Pública | Pública; o validador só retorna dados se existir um certificado com o código informado. |
| `GET` | `/api/events/{eventId}/certificate` | `EventCertificateIssuanceController#buildCertificateFromEvent` | Protegida (JWT) | Evento encerrado e não cancelado; exige inscrição e check-in no evento. |
| `GET` | `/api/events/activities/{activityId}/certificate` | `EventCertificateIssuanceController#buildCertificateFromActivity` | Protegida (JWT) | Exige inscrição ativa na atividade, check-in no evento e presença na atividade; não exige evento encerrado. |
| `GET` | `/api/events/{eventId}/instructors/{instructorId}/certificate` | `EventCertificateIssuanceController#buildCertificateFromInstructor` | Protegida (JWT) | Ministrante com vínculo confirmado e conta associada, ou equipe com permissão de edição; exige evento encerrado. |
| `GET` | `/api/events/{eventId}/activities/{activityId}/guests/{activityGuestId}/instructor-certificate` | `EventCertificateIssuanceController#buildCertificateFromActivityGuest` | Protegida (JWT) | O próprio convidado apresentador com conta vinculada ou equipe com permissão de edição; exige vínculo PRESENTER confirmado e evento encerrado. |
| `POST` | `/api/events/{eventId}/instructor-certificate/manual` | `EventCertificateManualController#buildManualInstructorCertificate` | Protegida (JWT) | Owner/editor ativo/MODERATOR/ADMIN; emite para vínculo de ministrante confirmado ou convidado PRESENTER confirmado; convidado sem conta é aceito. Não inclui código de validação. |
| `GET` | `/api/events/{eventId}/team-certificate` | `EventCertificateIssuanceController#buildCertificateForEditor` | Protegida (JWT) | Owner ou editor ativo; exige evento encerrado e carga horária da equipe configurada. |
| `POST` | `/api/events/{eventId}/team-certificate/manual/{recipientId}` | `EventCertificateManualController#buildManualTeamCertificate` | Protegida (JWT) | Owner/editor ativo/MODERATOR/ADMIN emite para owner ou editor ativo; exige evento encerrado e carga horária da equipe configurada. |
| `GET` | `/api/events/{eventId}/certificate-box-template` | `EventCertificateManagementController#getBoxTemplate` | Protegida (JWT) | Consulta as caixas/variáveis do certificado do evento (retorna o padrão se não salvo); exige canView no evento. |
| `GET` | `/api/events/{eventId}/activity-certificate-box-template` | `EventCertificateManagementController#getActivityBoxTemplate` | Protegida (JWT) | Consulta as caixas/variáveis do template único das atividades (retorna o padrão se não salvo); exige canView no evento. |
| `GET` | `/api/events/{eventId}/certificate-cover-template` | `EventCertificateManagementController#getCoverTemplate` | Protegida (JWT) | Consulta os bytes do fundo em uso (retorna o padrão se não salvo); exige canView no evento. |
| `GET` | `/api/events/{eventId}/certificate-preview` | `EventCertificateManagementController#previewEventCertificate` | Protegida (JWT) | Prévia PARTICIPANT/INSTRUCTOR/EDITOR com valores de exemplo; exige canView no evento. |
| `GET` | `/api/events/{eventId}/activity-certificate-preview` | `EventCertificateManagementController#previewActivityCertificate` | Protegida (JWT) | Prévia do template compartilhado de atividades com valores de exemplo; exige canView no evento. |
| `GET` | `/api/events/certificate-variables` | `EventCertificateManagementController#listCertificateVariables` | Protegida (JWT) | Lista as variáveis suportadas pelo contexto; não consulta dados de um evento. |
| `POST` | `/api/events/{eventId}/certificate-cover-template` | `EventCertificateManagementController#uploadCoverTemplate` | Protegida (JWT) | Upload de imagem válido (PNG/JPEG/WebP); exige canEdit no evento. |
| `DELETE` | `/api/events/{eventId}/certificate-cover-template` | `EventCertificateManagementController#deleteCoverTemplate` | Protegida (JWT) | 204 ao remover; 404 se não havia fundo salvo; exige canEdit no evento. |
| `POST` | `/api/events/{eventId}/certificate-box-template` | `EventCertificateManagementController#saveBoxTemplate` | Protegida (JWT) | Salva template PARTICIPANT/INSTRUCTOR/EDITOR; exige canEdit no evento. |
| `POST` | `/api/events/{eventId}/activity-certificate-box-template` | `EventCertificateManagementController#saveActivityBoxTemplate` | Protegida (JWT) | Salva o template único de atividades; exige canEdit no evento. |
| `POST` | `/api/clubs` | `ClubController#create` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/clubs/me` | `ClubController#findMyClubs` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/clubs/search` | `ClubController#search` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `DELETE` | `/api/clubs/{clubId}` | `ClubController#delete` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/clubs/{clubId}` | `ClubController#findById` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `PATCH` | `/api/clubs/{clubId}` | `ClubController#update` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/clubs/{clubId}/members/enroll` | `ClubMemberController#enroll` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/clubs/{clubId}/members/search` | `ClubMemberController#search` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/clubs/{clubId}/members/staff/{email}` | `ClubMemberController#addMember` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `DELETE` | `/api/clubs/{clubId}/members/unenroll` | `ClubMemberController#unenroll` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PATCH` | `/api/clubs/{clubId}/members/{memberId}/status` | `ClubMemberController#changeStatus` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events` | `EventsController#create` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/events/activities/{activityId}/conflicts` | `ActivitiesController#getConflictingActivities` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/activities/{activityId}/enrollments` | `ActivitiesController#getActivityEnrollments` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/activities/{activityId}/guests` | `GuestController#searchActivityGuests` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/activities/{activityId}/subscribe` | `ActivitiesController#subscribeToActivity` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `DELETE` | `/api/events/activities/{activityId}/unsubscribe` | `ActivitiesController#unsubscribeFromActivity` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `DELETE` | `/api/events/activities/{id}` | `ActivitiesController#deleteActivity` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PATCH` | `/api/events/activities/{id}` | `ActivitiesController#updateActivity` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PUT` | `/api/events/activities/{id}/capacity` | `ActivitiesController#updateActivityCapacity` | Protegida (JWT) | Owner/editor ativo/MODERATOR/ADMIN; envie maxCapacity positivo ou null para capacidade ilimitada. |
| `GET` | `/api/events/editors/accept` | `EditorsController#acceptEditorInvitation` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `POST` | `/api/events/invitations/{code}/reply` | `GuestInvitationController#replyToInvite` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `DELETE` | `/api/events/invitations/{invitationId}` | `GuestInvitationController#cancelInvitation` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/me/created` | `EventsMeController#getCreatedEvents` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/me/editors` | `EventsMeController#getMeEventsEditors` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/me/subscriptions` | `EventsMeController#getSubscriptions` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/search` | `EventsController#searchEvents` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `GET` | `/api/events/slug/{slug}` | `EventsController#getBySlug` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `DELETE` | `/api/events/{eventId}` | `EventsController#deleteEvent` | Protegida (JWT) | Owner, editor ativo, MODERATOR ou ADMIN; dados dependentes são removidos e a limpeza dos arquivos é solicitada após o commit. |
| `GET` | `/api/events/{eventId}` | `EventsController#getById` | Pública com acesso autenticado opcional | Eventos publicados são públicos; outros estados exigem JWT do owner/editor ativo/MODERATOR/ADMIN. Retorna `EventDTO`, incluindo `workloadHours`, `teamCertificateWorkloadHours` e `organization`. |
| `PATCH` | `/api/events/{eventId}` | `EventsController#updateEvent` | Protegida (JWT) | Owner, editor ativo, MODERATOR ou ADMIN; campos enviados incluem cargas horárias e organização. Campos omitidos ou `null` permanecem inalterados. |
| `PUT` | `/api/events/{eventId}/capacity` | `EventsController#updateEventCapacity` | Protegida (JWT) | Owner/editor ativo/MODERATOR/ADMIN; envie maxCapacity positivo ou null para capacidade ilimitada. |
| `GET` | `/api/events/{eventId}/activities` | `ActivitiesController#getActivities` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/{eventId}/activities` | `ActivitiesController#createActivity` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/{eventId}/activities/my-subscriptions` | `ActivitiesController#getMySubscribedActivities` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/{eventId}/editors` | `EditorsController#getEditors` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `DELETE` | `/api/events/{eventId}/editors/{email}` | `EditorsController#removeEditor` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `POST` | `/api/events/{eventId}/editors/{email}` | `EditorsController#addEditor` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/events/{eventId}/enrollments` | `EnrollmentsController#searchEnrollments` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `POST` | `/api/events/{eventId}/credentials/scan` | `EventCredentialController#scan` | Protegida (JWT) | Proprietário/editor ativo/MODERATOR/ADMIN ou operador temporário concedido no evento. |
| `POST` | `/api/events/{eventId}/credentials/manual/search` | `EventCredentialController#search` | Protegida (JWT) | Proprietário/editor ativo/MODERATOR/ADMIN ou operador temporário; QueryDSL limita a 20 resultados e retorna `EnrollmentListItem` com `UserSummaryDTO` (e-mail completo). |
| `POST` | `/api/events/{eventId}/credentials/manual` | `EventCredentialController#manual` | Protegida (JWT) | Proprietário/editor ativo/MODERATOR/ADMIN ou operador temporário concedido no evento. |
| `POST` | `/api/events/{eventId}/credentials/{credentialId}/reprint` | `EventCredentialController#reprint` | Protegida (JWT) | Proprietário/editor ativo/MODERATOR/ADMIN ou operador temporário concedido no evento. |
| `GET` | `/api/events/{eventId}/credentials/summary` | `EventCredentialController#summary` | Protegida (JWT) | Proprietário/editor ativo/MODERATOR/ADMIN ou operador temporário concedido no evento. |
| `POST` | `/api/events/{eventId}/credentials/my-token` | `EventCredentialController#myToken` | Protegida (JWT) | O participante autenticado só recebe token da própria inscrição elegível. |
| `POST` | `/api/events/{eventId}/credentials/my-token/rotate` | `EventCredentialController#rotateMyToken` | Protegida (JWT) | O participante autenticado só rotaciona o token da própria inscrição elegível. |
| `POST` | `/api/events/{eventId}/credentials/enrollments/{enrollmentId}/token/rotate` | `EventCredentialController#rotateParticipantToken` | Protegida (JWT) | Proprietário/editor ativo/MODERATOR/ADMIN ou operador temporário concedido no evento. |
| `PUT` | `/api/events/{eventId}/credentials/operators/{userId}` | `EventCredentialController#addOperator` | Protegida (JWT) | Gestão do evento; usuário concedido precisa ter conta ativa. |
| `DELETE` | `/api/events/{eventId}/credentials/operators/{operatorUserId}` | `EventCredentialController#removeOperator` | Protegida (JWT) | Gestão do evento. |
| `GET` | `/api/events/{eventId}/guests` | `GuestController#searchEventGuests` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PATCH` | `/api/events/{eventId}/guests/{guestId}` | `GuestController#updateGuest` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/{eventId}/invitations` | `GuestInvitationController#searchInvitations` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/{eventId}/invitations` | `GuestInvitationController#inviteUser` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PATCH` | `/api/events/{eventId}/status/{status}` | `EventsController#updateEventStatus` | Protegida (JWT) | Owner, editor ativo, MODERATOR ou ADMIN; as regras de transição de estado continuam aplicáveis. |
| `DELETE` | `/api/events/{eventId}/subscribe` | `EnrollmentsController#unsubscribe` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/{eventId}/subscribe` | `EnrollmentsController#subscribe` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/highlights` | `HighlightsController#highlights` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `GET` | `/api/highlights/clubs` | `HighlightsController#highlightsClubs` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `GET` | `/api/highlights/events` | `HighlightsController#highlightsEvents` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `GET` | `/api/highlights/news` | `HighlightsController#highlightsNews` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `GET` | `/api/news/admin/{id}` | `NewsController#getById` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `POST` | `/api/news/create` | `NewsController#create` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/news/me` | `NewsEditorController#getMyNews` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/news/search` | `NewsController#searchNews` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `PATCH` | `/api/news/{id}` | `NewsController#update` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `POST` | `/api/news/{id}/publish` | `NewsController#publish` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `DELETE` | `/api/news/{newsId}` | `NewsController#deleteById` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/news/{newsId}/editors` | `NewsEditorController#getEditors` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `DELETE` | `/api/news/{newsId}/editors/{email}` | `NewsEditorController#removeEditor` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/news/{newsId}/editors/{email}` | `NewsEditorController#addEditor` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/news/{slug}` | `NewsController#getBySlug` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `POST` | `/api/storage/upload` | `StorageController#uploadFile` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `DELETE` | `/api/storage/{fileName}` | `StorageController#deleteFile` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/storage/{fileName}` | `StorageController#getImage` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `DELETE` | `/api/users` | `UserProfileController#deactivateAccount` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PATCH` | `/api/users` | `UserProfileController#update` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/users/me` | `UserProfileController#getMe` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/swagger-ui/** e /v3/api-docs/**` | `SecurityConfig#docs` | Protegida (HTTP Basic) | Requer autenticação Basic com usuário da aplicação. |

## Princípios de cargos

| Cargo | Escopo |
|---|---|
| Usuário | Rotas autenticadas de participação e perfil próprio. |
| Staff | Cria conteúdo e gerencia somente recursos próprios ou onde é editor/instrutor. |
| Moderador | Permissões administrativas gerais e moderação entre proprietários; sem gestão de cargos. |
| Admin | Todas as ações, incluindo gestão de cargos. |

A rota `PUT /api/admin/users/{userId}/roles/{role}` exige somente `ADMIN`. O Grafana está ligado a localhost; para acesso remoto, use túnel SSH.
