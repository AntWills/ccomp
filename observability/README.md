# Observabilidade

Os arquivos dos containers são enviados ao Loki pelo Alloy. A descoberta mantém
somente containers com o rótulo de projeto Compose `ccomp`. Grafana/LGTM guarda
logs e métricas por 30 dias; os dados ficam nos volumes Docker persistentes.
Os logs recebem os rótulos `service_name` (serviço Compose) e `container`, para
filtrar por exemplo `ccomp-backend`, `postgres-db` ou `postgres-exporter`.
O endpoint da aplicação consulta pelo serviço Compose e agrega suas réplicas;
ele não recebe nem retorna nomes de containers.

## Consulta de logs de infraestrutura pela API

O endpoint `GET /api/admin/infra-logs` consulta o Loki internamente e exige um
JWT com perfil `ADMIN`. Ele retorna os logs em `logs`, separados dos registros
de auditoria disponíveis em `/api/admin/audit-logs`.

Filtros opcionais (também tipados e documentados no Swagger):

- `serviceName`: serviço Compose. Padrão `ccomp-backend`. Valores: `proxy`, `ccomp-backend`, `postgres-db`,
  `postgres-exporter`, `seaweeds`, `seaweeds-setup`, `rabbitmq`, `redis`,
  `redis-exporter`, `grafana-lgtm`, `prometheus` e `alloy`.
- `level`: nível normalizado pelo Alloy. Opções: `info`, `warn`, `error`,
  `fatal`, `panic`, `warning`, `log`, `notice`, `debug1` a `debug5`. O Alloy
  cria esse rótulo apenas para `proxy`, `postgres-db` e `seaweeds`; nos demais
  serviços esse filtro não encontra resultados.
- `search`: texto literal contido na mensagem.
- `start` e `end`: período ISO-8601; por padrão, última hora, com máximo de 7 dias.
- `limit`: quantidade máxima de linhas, de 1 a 500; padrão 100.
- `cursor`: valor de `nextCursor` da resposta anterior para carregar a próxima
  página de logs mais antigos. O cursor preserva o período original; mantenha os
  mesmos filtros ao navegar.

Quando houver mais resultados, a resposta inclui `nextCursor`; quando não houver
mais páginas, o campo será `null`.

Exemplo usando os valores padrão do serviço e período: `/api/admin/infra-logs`.
Exemplo de nível no PostgreSQL: `/api/admin/infra-logs?serviceName=postgres-db&level=error`.
Exemplo com navegação: `/api/admin/infra-logs?serviceName=postgres-db&level=error&limit=100&cursor=<nextCursor da resposta anterior>`.
No Swagger, `serviceName` e `level` aparecem como listas selecionáveis; `cursor`
é um campo de texto que recebe o `nextCursor` da resposta anterior.
O Loki não é publicado externamente; somente o backend acessa sua API pela rede
Docker.

## Métricas

O Alloy coleta métricas do backend (`/actuator/prometheus`, porta interna 8081),
PostgreSQL (`postgres-exporter`), RabbitMQ, Redis (`redis-exporter`) e SeaweedFS
a cada 15 segundos e as envia ao Prometheus. O endpoint Prometheus do backend
não é publicado no host; mantenha a porta 8081 acessível apenas pela rede Docker.
As métricas do backend incluem JVM, HTTP, processo, datasource/Hikari e pool de
conexões. Acompanhe em especial `hikaricp_connections_pending`,
`hikaricp_connections_active`, `hikaricp_connections_timeout_total` e a
distribuição `hikaricp_connections_acquire`.

O PostgreSQL inicia com `pg_stat_statements` pré-carregado. O script em
`observability/postgres/init` cria a extensão apenas quando o volume do banco é
inicializado pela primeira vez. Em uma instalação que já tem volume persistente,
após recriar o container para aplicar o preload, crie a extensão uma vez:

```sql
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

O exporter habilita as estatísticas por consulta, além de conexões, atividade,
estatísticas do banco, tabelas, WAL e checkpoints. O usuário configurado hoje
para o exporter é o mesmo usuário do banco definido em `.env`; em produção,
substitua-o por um usuário de monitoramento com `pg_monitor` antes de expor ou
compartilhar o exporter fora da rede interna.

O Alloy precisa acessar `/var/run/docker.sock` para descobrir containers e ler
seus logs. A montagem `:ro` impede alterações no arquivo do socket, mas não
restringe os comandos da API Docker disponíveis por ele. Trate o Alloy com o
mesmo nível de confiança do daemon Docker e não exponha sua porta de controle.

O Grafana fica disponível apenas em loopback nos Compose. Para acesso remoto,
use um túnel SSH para a porta 3000 do host.
