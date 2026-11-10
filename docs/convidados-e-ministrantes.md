# Convidados e ministrantes

Este documento descreve como a plataforma representa as pessoas que participam de uma atividade e quais alterações cada uma pode fazer.

## Conceitos

**Convidado** é um perfil de destaque do evento, como um palestrante popular ou uma pessoa convidada especialmente. O perfil pertence ao evento e pode ser associado a uma ou mais atividades. Ele contém nome, descrição, imagem, status e visibilidade. A função em cada atividade é registrada no vínculo, por exemplo `PRESENTER`, `SPECIAL_GUEST`, `MODERATOR`, `ASSISTANT` ou `LISTENER`.

**Ministrante** é uma pessoa responsável por ministrar ou auxiliar em uma atividade, como um professor ou aluno conhecido pela comunidade. O registro pertence diretamente a uma atividade e contém nome, descrição, função (`INSTRUCTOR` ou `ASSISTANT`), status e, opcionalmente, uma conta da plataforma.

## Diferenças no modelo

| Aspecto | Convidado | Ministrante |
| --- | --- | --- |
| Onde o perfil pertence | Ao evento | À atividade |
| Associação com atividades | Um perfil pode participar de várias atividades | Cada registro representa uma atividade específica |
| Conta na plataforma | Opcional | Opcional |
| Imagem de perfil | Sim | Não há imagem associada ao registro de ministrante |
| Vínculo com conta | Pode ser associado a uma conta ativa | Pode ser informado no cadastro ou associado depois; a conta deve estar ativa |
| Exclusão disponível | Exclui o perfil do evento e seus vínculos | Exclui o registro da atividade indicada |

Mesmo que a mesma pessoa ministre mais de uma atividade, atualmente cada vínculo de ministrante é um registro independente. Excluir um registro não remove os registros correspondentes em outras atividades.

## Contas e permissões

Sem conta vinculada, a pessoa aparece nas listagens, mas não se autentica para editar dados ou atividades. A associação de conta é opcional e exige uma conta ativa.

Com conta vinculada, convidado ou ministrante pode editar o próprio perfil ou registro conforme as permissões abaixo. Para editar **a atividade específica à qual está associado** através da operação central de atualização de atividades, a conta precisa estar ativa e o vínculo precisa estar confirmado. Esse acesso não concede edição do evento nem de outras atividades.

O próprio convidado com conta vinculada também pode atualizar o nome, a descrição, o status e a visibilidade do seu perfil. Pode enviar ou remover a própria imagem. O perfil continua limitado ao evento ao qual pertence.

O ministrante pode cancelar o próprio vínculo confirmado com uma atividade. O cancelamento muda o status do vínculo; não apaga seu registro. A gestão de perfis, vínculos, convites e associações cabe a quem pode editar o evento, incluindo proprietário, editor autorizado, MODERATOR ou ADMIN conforme as regras de acesso da plataforma.

## Operações disponíveis

### Convidados

- `POST /api/events/{eventId}/guests`: cria um perfil no evento, sem exigir conta. `accountEmail` é opcional.
- `POST /api/events/{eventId}/guests/{guestId}/activities/{activityId}`: associa o perfil a uma atividade ou atualiza sua função nessa atividade.
- `PATCH /api/events/{eventId}/guests/{guestId}/account`: associa uma conta ativa e concede o vínculo com a atividade indicada no corpo.
- `PATCH /api/events/{eventId}/guests/{guestId}/profile`: altera nome e descrição. Permitido ao próprio convidado vinculado ou a um gestor do evento.
- `PATCH /api/events/{eventId}/guests/{guestId}`: altera status e visibilidade. Permitido ao próprio convidado vinculado ou a um gestor do evento.
- `POST` e `DELETE /api/events/{eventId}/guests/{guestId}/image`: enviam, substituem ou removem a imagem.
- `DELETE /api/events/{eventId}/guests/{guestId}`: exclui o perfil inteiro do evento, sua imagem e todos os vínculos com atividades.

Atualmente não há uma rota para remover apenas o vínculo de um convidado com uma atividade, preservando o perfil e as associações com as demais atividades.

### Ministrantes

- `POST /api/events/activities/{activityId}/instructors`: cria um registro para a atividade. `accountEmail` é opcional; quando informado, precisa corresponder a uma conta ativa.
- `PATCH /api/events/activities/{activityId}/instructors/{instructorId}/account`: vincula uma conta ativa a um registro existente.
- `POST /api/events/activities/{activityId}/instructors/invitations`: envia convite para ministrar a atividade; a pessoa precisa responder autenticada com a conta correspondente.
- `DELETE /api/events/activities/{activityId}/instructors/me`: o próprio ministrante cancela seu vínculo confirmado.
- `DELETE /api/events/activities/{activityId}/instructors/{instructorId}`: gestor do evento exclui o registro daquela atividade. Outros registros da mesma pessoa em outras atividades permanecem.

O cancelamento pelo ministrante e a exclusão pelo gestor têm efeitos diferentes: o primeiro mantém o vínculo com status cancelado; a segunda apaga o registro da atividade.

## Listagem de participantes de uma atividade

`GET /api/events/activities/{activityId}/instructors` reúne ministrantes e convidados associados à atividade. Cada item informa `isGuest` para distinguir a origem do registro e `role` para indicar sua função, inclusive `ASSISTANT` para quem auxilia.

As rotas de listagem de convidados também existem separadamente: `GET /api/events/{eventId}/guests` lista perfis do evento e `GET /api/events/activities/{activityId}/guests` lista os vínculos de convidados daquela atividade.

## Efeito da exclusão sobre os recursos

Excluir um convidado remove seu perfil e as referências de atividade associadas, além de apagar a imagem do storage quando houver. Excluir um ministrante remove somente o registro indicado. Nenhuma dessas operações exclui a atividade ou o evento.

## Perfis centrais de ministrante

Ministrantes agora possuem um perfil pertencente ao evento, associado a vínculos específicos de atividade. A rota legada `GET /api/events/{eventId}/instructors` continua listando vínculos por atividade; a lista central de perfis usa `GET /api/events/{eventId}/instructor-profiles`.

O perfil reúne nome, descrição e conta opcional; cada vínculo mantém sua própria função (`INSTRUCTOR` ou `ASSISTANT`) e status. Assim, a equipe lista uma pessoa uma vez no evento, mesmo quando ela ministra várias atividades.

As operações centrais permitem listar, criar, editar e excluir perfis, além de associá-los a atividades. Convites e associação de conta continuam disponíveis por atividade. Cancelar o próprio vínculo continua sendo uma ação por atividade; excluir um vínculo não remove o perfil nem os demais vínculos. Excluir o perfil remove todos os vínculos associados.

Na migração V40, os registros existentes mantêm vínculos, funções e status. Registros com conta são agrupados por evento e conta; registros sem conta não são unidos com base somente em nomes coincidentes. As rotas existentes por atividade continuam disponíveis.

As cargas gerais de ministrante e convidado usadas nos certificados gerais serão configurações distintas na entidade evento, aplicadas uma vez por certificado. Elas não pertencem aos perfis individuais nem aos vínculos de atividade. `Event.workloadHours` permanece destinado ao certificado geral de participante. Ver [Certificados de eventos](event-certificado.md) para os cálculos e critérios de aceite.
