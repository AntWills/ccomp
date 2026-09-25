# Relatório de estabilização

**Branch:** `developer` (base `origin/developer`)  
**Commit principal:** `6a2d04a`

## Mudanças

- Adicionado o cargo `MODERATOR`, com gestão de cargos restrita a `ADMIN`, e documentado o acesso às rotas.
- Implementadas permissões de Storage por cargo e propriedade, com migrations para permissões, ownership e índices.
- Padronizados templates HTML de e-mail, respostas de erro e variáveis de ambiente de exemplo.
- Configurados Alloy, Loki/Grafana, retenção de 30 dias e Redis para cache dos destaques públicos.
- Corrigidos nomes de pacotes e o typo `EventInvitationDspRepository`.

## Verificação

- Compose de desenvolvimento e produção validados.
- Build Maven concluído com `-DskipTests`; a suíte de testes não foi executada.
- Alterações permanecem em commits locais, sem push.
