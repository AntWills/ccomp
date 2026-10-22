# Observabilidade

Os arquivos dos containers são enviados ao Loki pelo Alloy. A descoberta mantém
somente containers com o rótulo de projeto Compose `ccomp`. Grafana/LGTM guarda
logs e métricas por 30 dias; os dados ficam nos volumes Docker persistentes.
Os logs recebem os rótulos `service_name` (serviço Compose) e `container`, para
filtrar por exemplo `ccomp-backend`, `postgres-db` ou `postgres-exporter`.

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
