# Busca textual por termo

Este documento apresenta decisões e componentes que podem ser usados para implementar busca textual por termo em diferentes áreas do projeto. A busca administrativa de usuários é um exemplo concreto; campos, limiares e filtros podem variar conforme cada domínio.

## Conceitos

Uma busca textual pode combinar correspondência parcial, tolerância a diferenças de digitação e ordenação por relevância. No PostgreSQL, a extensão `pg_trgm` oferece funções e operadores baseados em trigramas, grupos de três caracteres usados para comparar textos. Essa abordagem mantém dados e busca no banco relacional sem exigir um serviço externo para necessidades moderadas.

## Aplicação em outros domínios

Cada funcionalidade deve definir quais campos são pesquisáveis, como combinar seus scores, qual limiar aceita um resultado e como a relevância afeta a ordenação. Um critério apropriado para um domínio não é necessariamente adequado a outro; limiares e fórmulas não são universais.

`word_similarity(termo, campo)` compara o termo com a melhor parte correspondente de um texto maior, sendo útil para localizar trechos em nomes, e-mails, títulos ou outros campos. `similarity(termo, campo)` compara os trigramas dos textos completos. Uma implementação pode usar uma dessas métricas ou combiná-las, documentando a regra adotada.

## Exemplo atual: busca administrativa de usuários

O endpoint `POST /api/admin/users/search` aceita `searchTerm` junto aos filtros de status e cargo. O termo é comparado com nome e e-mail. Um usuário entra nos resultados quando `word_similarity(termo, nome)` **ou** `word_similarity(termo, email)` é pelo menos `0.4` (40%). Esse limiar é específico da busca administrativa de usuários.

Para ordenar esse exemplo, a aplicação soma `word_similarity` e `similarity` para cada campo e usa a maior soma entre nome e e-mail como score do usuário. A ordenação é por score decrescente, seguida por `createdAt` e ID decrescentes para desempatar. Essa fórmula é específica do exemplo e não precisa ser replicada por outras buscas.

Quando `searchTerm` está ausente, nulo ou vazio, não há busca textual nem ordenação por relevância: a listagem segue `createdAt` e ID decrescentes. Os filtros de status e cargo continuam aplicados.

### Busca pública de eventos

O endpoint `POST /api/events/search` também aceita `searchTerm`, aplicado somente ao título. O evento precisa estar publicado e continuar atendendo aos filtros opcionais de categoria e formato. O termo é aceito quando `word_similarity(termo, título) >= 0.4`; a relevância ordena por `word_similarity + similarity`, seguida de `startDate` e ID decrescentes. O cursor inclui essa pontuação enquanto a busca está ativa.

Sem termo, o score não é projetado nem calculado; a ordenação continua por `startDate` e ID decrescentes. A migration `V23__add_event_title_text_search.sql` cria o índice GIN trigramático do título. Assim como na busca de usuários, o filtro por função `word_similarity(...) >= 0.4` não garante que o índice seja escolhido; avalie o plano com `EXPLAIN (ANALYZE, BUFFERS)` em dados representativos.

## Índices e planos de consulta

O PostgreSQL permite criar índices GIN trigramáticos usando `gin_trgm_ops` para os campos pesquisáveis. No exemplo de usuários, a migração `V22__add_user_text_search.sql` habilita `pg_trgm` e cria índices para `tb_users.name` e `tb_users.email_address`.

Operadores trigramáticos como `%` e `<%` podem usar índices GIN. Comparar diretamente o resultado numérico de uma função, como `word_similarity(...) >= 0.4`, não garante que o otimizador use esses índices. Portanto, a presença de um índice não prova que determinada consulta esteja indexada. Verifique consultas representativas com `EXPLAIN (ANALYZE, BUFFERS)` e, se necessário, avalie operadores indexáveis que preservem a semântica e o limiar desejados.

Índices GIN ocupam espaço e aumentam o custo de escrita. A ordenação por uma pontuação calculada para cada termo também exige ordenar os resultados candidatos; um índice não armazena um único score de relevância, pois ele depende da busca atual.

## Paginação

Sem score, a paginação por cursor pode usar uma tupla estável como `(createdAt, id)`. Em buscas ordenadas por relevância, o cursor precisa incluir o score e os campos de desempate, por exemplo `(score, createdAt, id)`. A condição da página seguinte deve comparar a mesma tupla, respeitando a direção da ordenação.

O cliente deve continuar a paginação com o mesmo termo e os mesmos filtros. Como o score é calculado durante a consulta, mudanças nos dados entre requisições podem alterar posições. O cursor dá continuidade determinística ao estado observado, mas não cria uma fotografia imutável dos dados.

## Quando considerar outra solução

`pg_trgm` atende buscas parciais e tolerantes a variações sem sair do PostgreSQL. Se os requisitos incluírem relevância textual avançada, análise linguística, stemming, busca em documentos complexos ou uma escala que não se ajuste aos planos medidos, compare motores dedicados usando requisitos e dados reais do projeto.
