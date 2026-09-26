# Matriz de acesso às rotas

Gerada a partir dos controllers e do `SecurityConfig`. Rotas não listadas por anotação específica ficam protegidas pelo JWT global. A verificação de autoria e propriedade ocorre nos serviços; endpoints públicos de busca só retornam dados publicados.

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
| `GET` | `/api/events/editors/accept` | `EditorsController#acceptEditorInvitation` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `POST` | `/api/events/invitations/{code}/reply` | `GuestInvitationController#replyToInvite` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `DELETE` | `/api/events/invitations/{invitationId}` | `GuestInvitationController#cancelInvitation` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/me/created` | `EventsMeController#getCreatedEvents` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/me/editors` | `EventsMeController#getMeEventsEditors` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/me/subscriptions` | `EventsMeController#getSubscriptions` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/search` | `EventsController#searchEvents` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `GET` | `/api/events/slug/{slug}` | `EventsController#getBySlug` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `DELETE` | `/api/events/{eventId}` | `EventsController#deleteEvent` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/events/{eventId}` | `EventsController#getById` | Pública | Pública; o serviço filtra conteúdo não publicado quando aplicável. |
| `PATCH` | `/api/events/{eventId}` | `EventsController#updateEvent` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/events/{eventId}/activities` | `ActivitiesController#getActivities` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/{eventId}/activities` | `ActivitiesController#createActivity` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/{eventId}/activities/my-subscriptions` | `ActivitiesController#getMySubscribedActivities` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/{eventId}/editors` | `EditorsController#getEditors` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `DELETE` | `/api/events/{eventId}/editors/{email}` | `EditorsController#removeEditor` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `POST` | `/api/events/{eventId}/editors/{email}` | `EditorsController#addEditor` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/events/{eventId}/enrollments` | `EnrollmentsController#searchEnrollments` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
| `GET` | `/api/events/{eventId}/guests` | `GuestController#searchEventGuests` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PATCH` | `/api/events/{eventId}/guests/{guestId}` | `GuestController#updateGuest` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `GET` | `/api/events/{eventId}/invitations` | `GuestInvitationController#searchInvitations` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `POST` | `/api/events/{eventId}/invitations` | `GuestInvitationController#inviteUser` | Protegida (JWT) | Requer autenticação; regras de propriedade/participação são verificadas no serviço quando aplicável. |
| `PATCH` | `/api/events/{eventId}/status/{status}` | `EventsController#updateEventStatus` | Restrita por role | Roles: ADMIN, MODERATOR, STAFF. Políticas de propriedade também se aplicam. |
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
