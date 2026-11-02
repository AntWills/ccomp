# Relatório: materiais para eventos, atividades e clubes

**Data:** 31/10/2026  
**Escopo analisado:** backend Java/Spring, persistência PostgreSQL e integração S3/MinIO presentes neste repositório.

## Resumo

É viável adicionar materiais anexados a eventos, atividades e clubes aproveitando a integração S3/MinIO já existente. A recomendação é tratar materiais como uma coleção de arquivos anexados a cada entidade, com metadados no banco, endpoints próprios por entidade e autorização verificada no servidor em cada operação. Não se recomenda usar o endpoint genérico `/api/storage` para esse fluxo: hoje ele limita operações a ADMIN/MODERATOR e não associa o arquivo ao evento, atividade ou clube.

Este repositório contém o backend; não foram localizadas telas ou código de frontend. A experiência de interface deverá ser planejada no repositório cliente.

## Situação atual

- `Event` tem capa (`coverImageKey`) e acesso por proprietário, editor ativo e ADMIN/MODERATOR. O serviço de capa usa chaves organizadas no bucket, valida conteúdo e apaga a capa antiga após substituição.
- `EventActivity` pertence a um evento (`event_id`). Os serviços de atividade autorizam edição com a política do evento. A atividade não tem campo ou coleção para anexos.
- `Club` já tem texto, capa e membros. A política de clube expõe a verificação de instrutor ativo; o serviço de atualização também permite MODERATOR/ADMIN. Não existe fluxo dedicado de arquivo de clube.
- `StorageExternal` faz operações no S3/MinIO. `StorageKeyGenerator` cria chaves imprevisíveis sob um prefixo. O conteúdo do arquivo não fica no banco.
- `tb_storage_files` guarda apenas nome, proprietário e data; não guarda relação com entidade, tamanho, tipo ou nome original. O fluxo de capas usa `StorageExternal` e não essa tabela.

## Decisões de produto a definir

Antes da implementação, convém confirmar estes comportamentos; as recomendações abaixo servem como padrão inicial:

1. **Quem pode ver?** Padrão sugerido: materiais de evento publicado/não listado e suas atividades seguem a visibilidade já aplicada ao evento; material de rascunho segue as permissões de visualização do evento. Para clubes, conteúdo público publicado é visível publicamente; clube não publicado fica restrito à equipe autorizada. Se houver materiais exclusivos para inscritos/membros, isso precisa ser uma regra separada e explícita.
2. **Que tipos de arquivo?** Definir uma allowlist e limites de tamanho por arquivo. Para uma primeira versão, aceitar documentos comuns (PDF e formatos Office) e imagens, se houver necessidade; evitar aceitar qualquer tipo sem restrição.
3. **Quantos e por quanto tempo?** Definir limite de quantidade/tamanho por entidade e política de retenção ao apagar anexos, entidades ou substituir arquivos.
4. **Materiais são links ou uploads?** Este relatório considera upload de arquivos. Links externos podem ser uma funcionalidade distinta, com validação de URL e regras próprias.

## Alterações recomendadas

### 1. Persistência

Criar uma migration Flyway nova, sem alterar migrations já aplicadas, para registrar os anexos. Cada registro deve conter pelo menos:

- identificador;
- chave privada do objeto no storage;
- nome original apresentado ao usuário;
- tipo de conteúdo validado;
- tamanho em bytes;
- data de criação e usuário que enviou;
- relação obrigatória com o evento, atividade ou clube;
- opcionalmente, descrição/legenda e posição de ordenação.

Para preservar integridade referencial, prefira tabelas de associação por tipo (`tb_event_materials`, `tb_activity_materials`, `tb_club_materials`), com FK para a entidade correspondente e índices por entidade/data. Podem compartilhar uma classe base ou componente Java para metadados. Uma única tabela polimórfica com `owner_type` e `owner_id` é mais curta, mas não consegue expressar uma FK convencional para os três tipos e facilita referências órfãs.

Definir `ON DELETE CASCADE` para metadados associados à entidade, mas lembrar que exclusão em PostgreSQL não remove objetos no S3. O serviço de exclusão deve remover objetos ou gerar tarefa de limpeza confiável.

### 2. API e serviços

Criar endpoints dedicados, com nomes consistentes, por exemplo:

| Operação | Evento | Atividade | Clube |
|---|---|---|---|
| Listar | `GET /api/events/{eventId}/materials` | `GET /api/activities/{activityId}/materials` | `GET /api/clubs/{clubId}/materials` |
| Enviar | `POST` na mesma coleção, multipart | idem | idem |
| Baixar | `GET .../materials/{materialId}/file` | idem | idem |
| Excluir | `DELETE .../materials/{materialId}` | idem | idem |

Os caminhos são uma proposta; o controller atual de atividades usa `/api/events/activities/{id}`, então a implementação pode conservar esse padrão para não criar inconsistência. A resposta de listagem deve conter metadados, nunca bytes do arquivo. A paginação pode ser adicionada se o volume esperado justificar.

Reaproveitar `StorageExternal` e `StorageKeyGenerator`, usando prefixos como `events/{id}/materials/`, `activities/{id}/materials/` e `clubs/{id}/materials/`. O arquivo deve ser enviado ao storage e seu registro salvo; se a gravação no banco falhar depois do upload, remover o objeto recém-criado para evitar órfãos. Ao excluir, evitar remover o objeto antes de confirmar que a operação está autorizada e que o registro corresponde à entidade da rota.

### 3. Autorização e privacidade

- **Evento:** reutilizar `EventAccessPolicy`. Exigir `canEdit` para upload e exclusão; para listagem/download exigir `assertCanView`, respeitando público, não listado, rascunho e cancelado conforme a política atual.
- **Atividade:** resolver a atividade e seu evento e aplicar a política do evento. Não confiar apenas no `activityId` ou em um `eventId` fornecido pelo cliente; confirmar o vínculo entre os dois. Para edição, aplicar a mesma regra que hoje permite ao proprietário/editor ativo/moderador/administrador editar a atividade.
- **Clube:** ampliar `ClubAccessPolicy` com operações explícitas de visualizar e editar ou criar um serviço equivalente. Para edição, refletir a regra atual do clube (instrutor ativo ou moderador/administrador, considerando os papéis de equipe já tratados pelo serviço). Para leitura, respeitar `isPublic()` e restringir clube não publicado. Não presumir que todo membro deve poder enviar ou remover materiais.
- Revalidar autorização também no download. Uma URL pública fixa do S3 pode contornar as regras de visibilidade. Preferir streaming autenticado pelo backend ou URL pré-assinada curta após autorização.

### 4. Validação e operação do storage

- Validar arquivo não vazio, tamanho máximo, extensão e MIME; não confiar só no `Content-Type` enviado pelo cliente. Para riscos de conteúdo malicioso, considerar verificação por assinatura e antivírus conforme a infraestrutura disponível.
- Guardar a chave interna, não URL pública, como referência canônica. Não permitir que o cliente escolha a chave do storage.
- Definir cabeçalhos seguros de download (`Content-Disposition`) e tratar nomes originais para evitar injeção em cabeçalhos.
- Implementar limpeza de objetos órfãos e comportamento definido para falhas parciais entre PostgreSQL e S3, pois não há transação distribuída entre os dois.
- Revisar `StorageController`: o download e exclusão genéricos recebem JWT mas não usam o usuário para validar proprietário; não reutilizar esses endpoints como API de materiais sem corrigir esse modelo.

### 5. DTOs, cache e documentação

Criar DTO de material com ID, nome, tipo, tamanho, data de envio, autor e URL/rota de download autorizada. Não expor chave interna do bucket. Atualizar OpenAPI, exemplos, mensagens de erro e matriz de acesso (`docs/access-control-matrix.md`). Se a listagem de evento/atividade/clube ou seus detalhes forem cacheados, invalidar os caches correspondentes após incluir ou remover material.

## Sequência de entrega sugerida

1. Fechar regras de visibilidade, formatos, limites e exclusão.
2. Criar migration e entidades/metadados de material.
3. Implementar serviço comum de upload/download/exclusão com compensação para falha parcial e validação.
4. Implementar autorização específica para evento, atividade e clube e os endpoints correspondentes.
5. Atualizar DTOs, OpenAPI, matriz de acesso e documentação do cliente.
6. No frontend, incluir lista, upload, estado de progresso, erros de limite, download e remoção conforme permissões.
7. Cobrir permissões (incluindo anônimo, usuário sem vínculo, membro/instrutor, proprietário/editor e moderador), limites, falhas de storage e exclusão em cascata com testes automatizados.

## Riscos e pontos de atenção

- A relação entre metadados PostgreSQL e objetos S3 não é atômica; precisa de compensação e/ou rotina de reconciliação.
- Materiais podem conter dados privados; autorização só na listagem não basta, o download também precisa ser controlado.
- Exclusão de eventos/atividades/clubes exige cuidado para não deixar objetos órfãos nem apagar arquivo antes da validação de acesso.
- Os limites atuais do proxy de upload e configurações de multipart devem ser conferidos antes de suportar arquivos grandes.
- O relatório cobre o backend presente neste workspace; endpoints e telas do frontend ficam fora da inspeção.

## Conclusão

O desenho mais coerente com o código é uma funcionalidade de anexos com metadados no PostgreSQL, conteúdo no S3/MinIO e autorização delegada às políticas de evento, atividade e clube. O fluxo existente de imagem de capa demonstra como integrar storage e regra de acesso, mas materiais pedem registros próprios, múltiplos arquivos por entidade, metadados e regras de download. A principal definição pendente de produto é se materiais serão públicos junto com a entidade ou limitados a inscritos/membros.
