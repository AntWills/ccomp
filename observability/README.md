# Observabilidade

Os arquivos dos containers são enviados ao Loki pelo Alloy. A descoberta mantém
somente containers com o rótulo de projeto Compose `ccomp`. Grafana/LGTM guarda
logs e métricas por 30 dias; os dados ficam nos volumes Docker persistentes.

O Alloy precisa acessar `/var/run/docker.sock` para descobrir containers e ler
seus logs. A montagem `:ro` impede alterações no arquivo do socket, mas não
restringe os comandos da API Docker disponíveis por ele. Trate o Alloy com o
mesmo nível de confiança do daemon Docker e não exponha sua porta de controle.

O Grafana fica disponível apenas em loopback nos Compose. Para acesso remoto,
use um túnel SSH para a porta 3000 do host.
