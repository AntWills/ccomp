# Materiais de eventos e atividades: decisões e integração com o frontend

**Atualizado em:** 02/11/2026

**Escopo:** endpoints e serviços backend de materiais para eventos e atividades. O subdomínio de clubes ainda não possui implementação de materiais.

## Estado da funcionalidade

O backend permite associar links e arquivos a eventos e atividades. Os metadados são persistidos no PostgreSQL em tabelas separadas; os arquivos são guardados no S3/MinIO. Os controllers verificam permissão em cada operação, inclusive no download.

As decisões abaixo descrevem o comportamento implementado. A integração de frontend pode usar os endpoints e regras deste documento. Materiais de clubes não devem ser integrados ainda: os caminhos e regras apresentados anteriormente como proposta não foram implementados.

## Decisões implementadas

| Tema | Decisão atual |
|---|---|
| Entidades atendidas | Eventos e atividades de eventos. Não há materiais para clubes. |
| Tipos | `LINK`, `FILE` e `IMAGE`. |
| Links | Armazenados como URL HTTP ou HTTPS, validados no backend. |
| Arquivos | PDF, PNG, JPEG e WebP; máximo de 10 MiB por arquivo; assinatura do conteúdo conferida. |
| Visibilidade | Materiais seguem a visibilidade do evento pai. `PUBLISHED` e `UNLISTED` podem ser vistos publicamente; `DRAFT` e `CANCELED` exigem vínculo de gestão. |
| Edição | Proprietário, editor ativo, `MODERATOR` ou `ADMIN`. Ser participante, inscrito ou membro não concede permissão específica para materiais. |
| Listagem | Cursor descendente por data de criação e ID. Página padrão de 10, máximo 50. |
| Download | Streaming autenticado pelo backend e sujeito novamente à regra de visualização. |
| Remoção | Metadado excluído no banco; objeto de storage apagado depois do commit. |
| Chaves do storage | Geradas pelo backend e não retornadas nos DTOs. |

## Controllers e rotas

Os dois controllers oferecem a mesma forma de API. A diferença é o recurso pai: evento ou atividade. O caminho de atividade existente inclui o segmento `/events/activities`; não use `/api/activities`.

| Operação | Evento | Atividade |
|---|---|---|
| Listar | `GET /api/events/{eventId}/materials` | `GET /api/events/activities/{activityId}/materials` |
| Criar link | `POST /api/events/{eventId}/materials/links` | `POST /api/events/activities/{activityId}/materials/links` |
| Enviar arquivo | `POST /api/events/{eventId}/materials/files` | `POST /api/events/activities/{activityId}/materials/files` |
| Baixar arquivo | `GET /api/events/{eventId}/materials/{materialId}/file` | `GET /api/events/activities/{activityId}/materials/{materialId}/file` |
| Remover material | `DELETE /api/events/{eventId}/materials/{materialId}` | `DELETE /api/events/activities/{activityId}/materials/{materialId}` |

### Listagem e paginação

Os GETs de coleção recebem:

- `pageSize`: opcional, padrão `10`, máximo efetivo `50`;
- `nextCursor`: token opaco recebido na resposta anterior, omitido na primeira página.

A resposta tem `content`, `next_cursor` e `previous_cursor`. O frontend deve enviar o valor de `next_cursor` como `nextCursor` na próxima requisição, mantendo o mesmo tamanho de página. O backend busca uma linha a mais que o limite solicitado para determinar se existe próxima página. `previous_cursor` é atualmente `null`; não há navegação para página anterior.

Os registros são ordenados por `created_at` decrescente e, em caso de empate, por `id` decrescente. O cursor representa os dois campos. O frontend deve tratá-lo como opaco e reiniciar a paginação quando o usuário mudar de evento/atividade ou invalidar a lista após uma gravação ou remoção.

Exemplo de resposta:

```json
{
  "content": [
    {
      "id": 81,
      "title": "Material de apoio",
      "description": null,
      "type": "FILE",
      "url": null,
      "original_file_name": "apoio.pdf",
      "content_type": "application/pdf",
      "size_bytes": 245760,
      "created_at": "2026-11-02T12:30:00"
    }
  ],
  "next_cursor": "token-opaco-ou-null",
  "previous_cursor": null
}
```

Os nomes JSON usam `snake_case`. A resposta não contém o autor (`created_by`), a chave do S3 nem uma URL pública do arquivo. Para baixar um `FILE` ou `IMAGE`, o frontend monta a rota `/materials/{id}/file` sob o recurso pai correspondente. Um `LINK` usa o campo `url` e não deve chamar a rota de arquivo.

### Criar link

Envie `application/json` para `/links`:

```json
{
  "title": "Documentação",
  "description": "Leitura recomendada",
  "url": "https://example.org/docs"
}
```

`title` é obrigatório e limitado a 255 caracteres; `description` é opcional e limitado a 1000; `url` é obrigatória e limitada a 2048. A URL deve ser absoluta, ter host e usar `http` ou `https`. O backend remove espaços em volta da URL e do título antes de salvar.

O sucesso retorna `201 Created` e `MaterialResponse`. Para links, `type` é `LINK`, `url` é preenchida e os metadados de arquivo ficam nulos.

### Enviar arquivo

Envie `multipart/form-data` para `/files` com estes campos:

| Campo | Obrigatório | Conteúdo |
|---|---|---|
| `title` | Sim | Título do material, até 255 caracteres. |
| `file` | Sim | PDF, PNG, JPEG ou WebP, não vazio e até 10 MiB. |
| `description` | Não | Descrição com até 1000 caracteres conforme o limite persistido. |

O backend verifica o `Content-Type` recebido e compara a assinatura dos bytes com o formato declarado. Configure o tipo MIME correto no multipart; extensão de nome não substitui a validação de conteúdo. O nome original é guardado após substituir barras, contrabarras e quebras de linha por `_`.

Uma imagem gera `type: IMAGE`; PDF gera `type: FILE`. A resposta `201 Created` inclui o ID e os metadados, mas não inclui bytes, chave ou URL do storage. O endpoint retorna o registro após o envio ao storage e a gravação no banco.

### Baixar arquivo

Faça `GET` em `/{materialId}/file` sob o evento ou atividade. O controller transmite o conteúdo com o tipo MIME recebido do storage e `X-Content-Type-Options: nosniff`. O download repete a checagem de visualização; não dependa de uma autorização feita apenas na listagem.

Para `LINK`, ou se o material/objeto não existir, a rota retorna `404`. O frontend deve abrir o `url` do link diretamente. O backend não define `Content-Disposition`; portanto, o navegador pode exibir tipos como PDF ou imagem em vez de sempre iniciar um download.

### Remover material

Envie `DELETE` para `/{materialId}` sob o evento ou atividade. O sucesso retorna `204 No Content`. A rota confere que o material pertence ao pai identificado no caminho; IDs de materiais de outro evento/atividade não podem ser removidos por essa rota.

## Acesso e visibilidade

As rotas GET de coleção e de arquivo são `permitAll` na configuração HTTP, para permitir acesso anônimo a eventos públicos. Isso não torna todos os materiais públicos: os serviços sempre consultam o evento e aplicam `EventAccessPolicy`.

| Estado do evento pai | Listar e baixar | Criar link/arquivo e remover |
|---|---|---|
| `PUBLISHED` | Público, inclusive anônimo. | Proprietário, editor ativo, `MODERATOR` ou `ADMIN`. |
| `UNLISTED` | Público para quem conhece o identificador/rota; não é indexável como evento publicado. | Proprietário, editor ativo, `MODERATOR` ou `ADMIN`. |
| `DRAFT` | Proprietário, editor ativo, `MODERATOR` ou `ADMIN`. | Proprietário, editor ativo, `MODERATOR` ou `ADMIN`. |
| `CANCELED` | Proprietário, editor ativo, `MODERATOR` ou `ADMIN`. | Proprietário, editor ativo, `MODERATOR` ou `ADMIN`. |

As permissões para materiais de atividade são herdadas do evento ao qual a atividade pertence. O controller recebe apenas `activityId`; o serviço resolve a atividade, obtém seu `eventId` e aplica a mesma política. Não há regra própria para instrutores, inscritos, convidados ou membros de clube: só têm acesso se também satisfizerem uma das permissões acima ou se o evento for publicamente visível.

POST e DELETE não são públicos na configuração de segurança e exigem JWT. As permissões de domínio ainda são verificadas no serviço. `STAFF`, isoladamente, não é uma permissão de edição de materiais.

## Modelo e lógica do backend

Evento e atividade têm tabelas separadas: `tb_event_materials` referencia `tb_events`; `tb_activity_materials` referencia `tb_event_activities`. Ambas armazenam título, descrição, tipo, URL ou metadados do arquivo, `created_by` e `created_at`. As FKs usam `ON DELETE CASCADE` para remover os metadados quando o pai é excluído. As constraints impedem combinar URL e arquivo no mesmo registro e aceitam `LINK`, `FILE` ou `IMAGE`.

As chaves seguem os prefixos `events/{eventId}/materials/` e `activities/{activityId}/materials/`, com identificador gerado pelo backend. O DTO não expõe a chave. O frontend não deve formar URLs do bucket nem usar o endpoint genérico `/api/storage` para esses materiais.

No upload, após autorização e validação, o serviço envia o arquivo ao storage, registra uma compensação para removê-lo se a transação do banco sofrer rollback e salva os metadados. Na exclusão, remove primeiro o registro dentro da transação e agenda o apagamento do objeto para depois do commit. As falhas de limpeza no storage são registradas em log e não revertidas para o cliente; não existe outbox ou rotina de reconciliação implementada.

## Decisões que o frontend deve respeitar

- Exibir ações de criar/remover somente para usuários que possam editar. O frontend pode usar o contexto de evento para ocultar ações, mas a resposta do backend é a autoridade final.
- Permitir leitura anônima somente quando o evento estiver `PUBLISHED` ou `UNLISTED`; para os demais estados, encaminhar JWT do proprietário, editor ativo, moderador ou administrador.
- Separar visualmente link, arquivo e imagem com base em `type`.
- Para `LINK`, abrir `url`; para `FILE` e `IMAGE`, buscar a rota de arquivo do respectivo pai.
- Enviar arquivos em multipart com MIME correto, informar o limite de 10 MiB e tratar `400`, `401` e `403`.
- Após incluir ou excluir, atualizar a página de materiais e descartar cursores anteriores daquela lista.
- Não esperar autor, quantidade de download, posição configurável, limite de quantidade por evento/atividade, página anterior ou materiais de clube: esses recursos não fazem parte da API atual.

## Limitações e pontos operacionais

- A validação do serviço limita cada arquivo a 10 MiB. Não há configuração multipart explícita no backend; confirme também os limites do proxy/gateway para permitir esse tamanho acrescido das partes do formulário.
- Não há limite de quantidade de materiais por evento/atividade nem limite agregado de storage por entidade.
- Se a remoção do objeto no S3 falhar após o commit, o metadado já foi removido e pode restar um objeto órfão. Se a compensação de upload falhar após rollback, também pode restar um objeto órfão. A operação registra a falha, mas não há retentativa automática.
- Ao apagar evento ou atividade, o PostgreSQL remove metadados por cascade, mas a FK não apaga objetos no S3. O fluxo de exclusão do pai ainda precisa de uma estratégia de limpeza desses objetos.
- O backend não implementa materiais de clubes. Não há migration, controller, política de acesso ou contrato de frontend para esse recurso.

## Referências no código

- Controllers: `EventMaterialsController` e `ActivityMaterialsController`.
- Serviços: `EventMaterialsService` e `ActivityMaterialsService`.
- Persistência e cursor: `EventMaterialDslRepository`, `ActivityMaterialDslRepository` e `MaterialCursor`.
- Contratos: `CreateMaterialLinkRequest`, `MaterialResponse` e `EnumMaterialType`.
- Autorização: `EventAccessPolicy` e regras HTTP de `SecurityConfig`.
- Persistência SQL: migrations `V24__create_event_and_activity_materials.sql` e `V25__support_image_material_type.sql`.
