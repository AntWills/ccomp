# Certificados de eventos

Este documento reúne o funcionamento dos certificados: relacionamentos usados para identificar destinatários, critérios de emissão, templates, variáveis, validação pública e rotas disponíveis.

## 1. Visão geral

Os certificados são imagens PNG montadas sob demanda. A aplicação combina uma imagem de fundo com caixas de texto; os marcadores nas caixas são substituídos pelos dados do evento e da pessoa. As emissões automáticas elegíveis podem criar ou reutilizar um código público de validação. A imagem não é persistida como um arquivo de certificado.

Há quatro contextos de certificado: participante do evento, participante de atividade, ministrante e equipe. Além deles, a equipe pode emitir certificados personalizados usando caixas e valores enviados na requisição.

## 1.1. Estado atual, comportamento desejado e diferenças

As seções seguintes descrevem primeiro o que o código faz hoje e depois a especificação desejada. Os itens desta subseção são a referência para separar comportamento implementado de trabalho futuro.

### Já implementado

- O certificado geral de participante soma `Event.workloadHours` (quando configurado) à carga das atividades em que há presença registrada. Atividades sem carga somam zero.
- O certificado de atividade do participante usa o valor de carga horária da própria atividade.
- Há templates gerais separados para `PARTICIPANT`, `INSTRUCTOR` e `EDITOR`. Há também um único template de atividade por evento, compartilhado entre todas as atividades.
- O fundo próprio é único por evento e é compartilhado entre todos os tipos e atividades. Sem fundo próprio, usa-se a imagem padrão do sistema.
- O certificado geral de ministrante tem template distinto do certificado geral de participante. Seu cálculo soma a carga das atividades com vínculo confirmado. A consulta também soma as horas de atividades em que a pessoa tem vínculo de convidado `PRESENTER` confirmado.
- Os modelos de dados de atividade aceitam carga horária nula. Nas somas atuais, uma atividade sem carga contribui com zero.

### Especificação desejada

#### Inscritos

1. **Certificado geral do evento:** um único certificado geral por inscrito. A carga é a soma da carga do evento, se configurada, com as cargas de todas as atividades em que o inscrito teve presença registrada; atividades sem carga não contribuem. Fundo e caixas são compartilhados por todos os inscritos daquele evento.
2. **Certificado específico de atividade:** representa somente a carga configurada para aquela atividade. Cada atividade pode ter seu próprio fundo e suas próprias caixas. Se a atividade não tiver uma configuração específica, é usado o template padrão de atividade.

#### Ministrantes

1. **Certificado geral do evento:** usa identidade visual própria, separada do certificado geral dos inscritos. A carga soma as atividades ministradas com vínculo confirmado e a carga de atuação geral do ministrante no evento, quando configurada.
2. **Certificado específico de atividade:** representa a carga daquela atividade para o ministrante. Pode usar configuração própria da atividade; se não houver, usa o template padrão de atividade para ministrantes.

#### Convidados

1. **Certificado geral do evento:** segue a regra de ministrantes, com template geral próprio para convidados. A carga soma as atividades em que o convidado confirmado tem vínculo `PRESENTER` e a carga geral de atuação do convidado no evento, quando configurada.
2. **Certificado específico de atividade:** representa a carga daquela atividade para o convidado apresentador. Pode usar configuração própria da atividade; se não houver, usa o template padrão de atividade para convidados.

#### Variáveis e resolução do template

Variáveis comuns, como título/nome do evento, organização, local, nome da pessoa e validação, devem ser reutilizáveis nos contextos compatíveis. Variáveis específicas devem distinguir participante, ministrante e convidado, inclusive suas cargas horárias.

A regra de resolução esperada é: **template específico da atividade e do contexto → template geral do evento para o mesmo contexto → template padrão do sistema**. Isso permite personalização por atividade e ainda mantém uma configuração própria do evento como alternativa. Fundo e caixas são resolvidos juntos na mesma camada para evitar combinar acidentalmente o fundo de uma atividade com caixas de outra.

### O que ainda falta para chegar à especificação

| Necessidade | Situação atual | Trabalho necessário |
|---|---|---|
| Template de atividade individual | Há um template único por evento | Persistir e consultar templates por atividade e contexto (participante, ministrante e convidado), com fallback. |
| Fundo por atividade | Há um único fundo por evento | Permitir imagem de fundo no template específico da atividade e aplicar a mesma ordem de fallback das caixas. |
| Template geral de convidado | Convidados usam `INSTRUCTOR` | Criar contexto e configuração geral próprios para convidado, coerentes com suas variáveis específicas. |
| Certificado de atividade para ministrante | Não há rota de emissão por atividade para ministrante | Implementar elegibilidade, carga horária e emissão usando o template resolvido para a atividade e contexto. |
| Certificado de atividade para convidado | Não há rota de emissão por atividade para convidado; o convidado usa emissão geral de ministrante | Implementar emissão específica para vínculo `PRESENTER`, incluindo convidado sem conta na emissão manual. |
| Carga geral de ministrante/convidado | Os campos específicos já existem em `Event` e podem ser definidos na criação/edição do evento; o cálculo do certificado ainda soma apenas atividades | Somar a carga geral correspondente no certificado geral e na validação, sem reaproveitar `Event.workloadHours`. |
| Variáveis comuns e específicas | As listas são separadas por contexto; convidado reutiliza variáveis de ministrante | Definir nomes e regras das variáveis de convidado e garantir a extração, validação, prévia e preenchimento real em cada contexto. |
| Validação dos novos certificados | Há códigos para evento, atividade, ministrante e equipe | Garantir identificação do vínculo/contexto específico para ministrante e convidado por atividade; manter verificação pública coerente com a carga impressa. |

### Caso 1 — carga geral de ministrante e convidado

**Objetivo:** permitir que um ministrante ou convidado apresentador tenha uma carga de atuação geral no evento, além das horas das atividades específicas.

**Decisão de domínio:** as cargas gerais destinadas a ministrantes e convidados pertencem à entidade `Event`, como configurações do evento, e não aos perfis individuais. O evento terá valores próprios para carga geral de ministrante e carga geral de convidado, ambos opcionais, não negativos e configurados pela equipe. `Event.workloadHours` continua sendo a carga geral usada no certificado de participante e não é reaproveitada nesses contextos. Sem carga específica configurada no evento, o valor contribui com zero.

**Cálculo do certificado geral:**

- Ministrante: carga geral de ministrante configurada no evento + soma das cargas das atividades em que seu vínculo estiver confirmado.
- Convidado: carga geral de convidado configurada no evento + soma das cargas das atividades em que seu vínculo `PRESENTER` estiver confirmado e a atividade tiver carga configurada.
- Carga nula de atividade contribui com zero. Vínculos cancelados ou não confirmados não contribuem.
- Em certificados específicos de atividade, é considerada somente a carga da atividade; a carga geral configurada no evento não é adicionada.

**Persistência:** a entidade/tabela de evento possui dois campos distintos, um para carga geral de ministrante e outro para carga geral de convidado. Eles são aceitos nas operações de criação e edição do evento. Cada valor deve ser aplicado uma única vez no cálculo geral do destinatário, independentemente de quantos vínculos confirmados ele tenha. O perfil central de ministrante do Caso 1.1 não armazena essa carga; `EventGuest` também não recebe esse campo.

**Transição dos dados atuais:** os novos campos de carga do evento começam sem valor (`NULL`, interpretado como zero); a migração não deve copiá-los de `Event.workloadHours` nem inferi-los das cargas das atividades. Os registros de ministrante atuais devem ser preservados; qualquer associação automática de registros existentes a um perfil central deve ser feita somente quando a identidade puder ser determinada sem ambiguidade. Registros sem conta não devem ser unidos apenas por nomes iguais.

**Critérios de aceite:** a equipe pode configurar e remover as cargas gerais de ministrante e convidado no evento; valores negativos são rejeitados; a carga correspondente ao contexto é aplicada uma única vez nos certificados gerais e na validação pública; certificados por atividade continuam mostrando apenas a carga da atividade; eventos sem carga específica configurada permanecem válidos.

### Caso 1.1 — cadastro e gestão central de ministrantes

**Objetivo:** oferecer uma lista central de ministrantes do evento e permitir cadastrar, consultar, editar e remover perfis, de forma semelhante à gestão de convidados. O cadastro central representa a pessoa no evento; vínculos a atividades representam onde e em qual função ela atua.

**Modelo implementado:** perfil de ministrante pertencente ao evento, contendo nome, descrição e conta opcional, com vínculos a atividades que preservam função (`INSTRUCTOR` ou `ASSISTANT`) e status. A pessoa pode estar associada a várias atividades sem duplicar seu perfil. A carga geral dos certificados permanece na entidade evento, conforme o Caso 1.

**Operações esperadas:**

- `GET /api/events/{eventId}/instructor-profiles`: listar todos os perfis centrais do evento, incluindo os sem vínculos;
- `POST /api/events/{eventId}/instructor-profiles`: criar perfil, opcionalmente vinculado a uma conta ativa;
- `PATCH /api/events/{eventId}/instructor-profiles/{profileId}`: editar nome e descrição;
- `PATCH /api/events/{eventId}/instructor-profiles/{profileId}/account`: vincular uma conta ativa;
- `DELETE /api/events/{eventId}/instructor-profiles/{profileId}`: excluir o perfil e seus vínculos;
- `POST /api/events/{eventId}/instructor-profiles/{profileId}/activities/{activityId}`: associar o perfil a uma atividade;
- remover vínculos com as rotas existentes por atividade; a função é definida ao associar o perfil;
- enviar convites para atividade pelas rotas existentes; cancelar o próprio vínculo confirmado e permitir que a equipe reative ou remova vínculos;
- manter as rotas existentes de consulta por atividade e de listagem combinada de ministrantes e convidados.

**Compatibilidade e migração:** a migração V39 cria os perfis centrais e associa os vínculos existentes. Registros com conta são agrupados por evento e conta; registros sem conta permanecem perfis separados, mesmo com nomes iguais. As rotas anteriores por atividade permanecem disponíveis. A operação de gestão central não transforma convidados com papel `PRESENTER` em ministrantes: são cadastros distintos e aparecem juntos apenas nas listagens combinadas de atividade.

**Critérios de aceite:** a equipe consegue ver todos os perfis do evento, inclusive os ainda sem atividade; uma pessoa com vários vínculos aparece uma vez na listagem central; as funções/status continuam específicos por atividade; exclusão de um vínculo não apaga o perfil nem os outros vínculos; exclusão do perfil remove seus vínculos; permissões de visualização/edição seguem as regras do evento.

### Plano de implementação sugerido

1. **Concluídas:** adicionar ao evento as cargas gerais distintas de ministrante e convidado e implementar perfis centrais/vínculos de ministrante com migração preservadora.
2. Centralizar o cálculo de carga por destinatário/contexto; aplicar a carga geral apenas nos certificados gerais e alinhar emissão, variáveis e validação.
3. Definir a matriz de tipos de template e a regra de fallback: atividade/contexto, evento/contexto e padrão do sistema.
4. Modelar configuração de atividade com contexto, caixas e fundo opcionais; adicionar migração compatível com eventos existentes.
5. Implementar emissão geral e por atividade de ministrantes e convidados, com rotas automáticas/manuais e regras de vínculo confirmado.
6. Atualizar prévias, consulta de variáveis, documentação das rotas e testes para cobrir templates próprios, fallback e cargas ausentes.

### Organização interna planejada

A refatoração manterá o domínio único `domain/events/certificate`. Não serão criados subdomínios de certificado. A estrutura planejada separa as camadas existentes por fluxo:

```text
certificate/
  web/
    issuance/
    management/
    validation/
  application/
    issuance/
    management/
    templates/
    variables/
    validation/
  persistence/
  dto/
  enums/
```

Os controllers de emissão, gestão e validação ficarão, respectivamente, em `web/issuance`, `web/management` e `web/validation`. Os serviços correspondentes ficarão em `application/issuance`, `application/management` e `application/validation`. Resolução de templates e carregamento de variáveis ficarão em `application/templates` e `application/variables`. Classes auxiliares compartilhadas poderão permanecer diretamente em `application` ou ser agrupadas em `application/shared`, conforme a responsabilidade.

Essa é a organização alvo; ela não implica que os arquivos já tenham sido movidos. A separação dos serviços por tipo de destinatário será feita dentro dessas pastas quando os fluxos tiverem regras próprias, mantendo componentes comuns para renderização, cálculo de horas e resolução de templates.

## 2. Relacionamentos que determinam o destinatário

```text
Evento
├── inscrição de participante ── conta de usuário
├── atividade
│   ├── inscrição de atividade ── inscrição no evento/conta
│   ├── vínculo de ministrante ── atividade; conta opcional
│   └── vínculo de convidado ── perfil de convidado do evento
│       └── papel PRESENTER; conta opcional
└── proprietário e editores ── contas de usuário
```

- **Participante do evento:** o destinatário é a conta autenticada associada à inscrição no evento.
- **Participante de atividade:** o destinatário é a conta associada à inscrição naquela atividade, que também referencia a inscrição no evento.
- **Ministrante:** cada registro de ministrante pertence a uma atividade. A mesma pessoa pode ter registros diferentes em várias atividades. Para emissão automática, o vínculo precisa estar confirmado e associado a uma conta. Para emissão manual, o vínculo confirmado pode não ter conta.
- **Convidado apresentador:** o perfil pertence ao evento e pode ser associado a várias atividades. O papel é definido em cada vínculo com atividade. Só o papel `PRESENTER`, com convidado confirmado, dá acesso ao certificado de ministrante. Para autoemissão é necessária uma conta vinculada; a equipe pode emitir manualmente mesmo sem conta.
- **Equipe:** o destinatário precisa ser proprietário (`owner`) do evento ou editor ativo. Moderadores e administradores podem atuar como emissores manuais quando a regra da rota permite, mas não se tornam destinatários por isso.

## 3. Emissão automática

As rotas automáticas exigem JWT. Elas retornam a imagem PNG para download. A elegibilidade é verificada no servidor; conhecer o identificador de evento, atividade ou vínculo não basta.

### Certificado do evento

`GET /api/events/{eventId}/certificate`

Emite para a própria pessoa autenticada. Ela precisa estar inscrita e com check-in no evento. O evento deve estar encerrado (`FINISHED`) e não cancelado. O credenciamento operacional não substitui o check-in exigido aqui.

A carga horária exibida é a carga horária configurada para o evento somada às cargas das atividades em que a pessoa registrou presença. O código de validação identifica o par evento, destinatário e tipo de certificado; chamadas posteriores reutilizam o código existente.

### Certificado de atividade

`GET /api/events/activities/{activityId}/certificate`

Emite para a própria pessoa autenticada. Exige inscrição ativa na atividade, check-in no evento e presença registrada especificamente naquela atividade. Não exige que o evento esteja encerrado. Usa o template de atividade do evento. A carga horária da atividade exibida é a carga horária configurada para ela; quando o template usa variáveis do evento, a carga do evento segue o cálculo do certificado de evento.

O código de validação identifica atividade e destinatário. A confirmação de presença por QR Code pode atualizar o check-in do evento e disparar o aviso por e-mail de que o certificado está disponível; credenciamento operacional continua sendo um fluxo separado.

### Certificado de ministrante

`GET /api/events/{eventId}/instructors/{instructorId}/certificate`

O vínculo de ministrante deve pertencer a uma atividade do evento, estar confirmado e ter uma conta associada. O próprio ministrante ou alguém com permissão para editar o evento pode solicitar a emissão. O evento deve estar encerrado e não cancelado.

A carga horária é somada entre as atividades do evento em que a conta tem vínculo confirmado como ministrante. O código de validação é específico para evento, pessoa e tipo `INSTRUCTOR`.

### Certificado de convidado apresentador

`GET /api/events/{eventId}/activities/{activityId}/guests/{activityGuestId}/instructor-certificate`

O vínculo precisa pertencer à atividade indicada, ter papel `PRESENTER` e apontar para um perfil de convidado confirmado. Para autoemissão, esse perfil precisa estar associado à conta autenticada. A equipe com permissão para editar o evento também pode solicitar a emissão. O evento deve estar encerrado e não cancelado.

O convidado usa o template de ministrante. A carga horária considera as atividades do evento nas quais sua conta aparece em vínculos confirmados de ministrante. O código é do tipo `INSTRUCTOR`, associado à conta.

### Certificado da equipe

`GET /api/events/{eventId}/team-certificate`

Emite para o próprio proprietário ou editor ativo autenticado. O evento deve estar encerrado e não cancelado, e sua carga horária de certificado da equipe deve estar configurada. Usa o template `EDITOR`; o código é associado ao destinatário e ao tipo `EDITOR`.

## 4. Emissão manual pela equipe

As rotas manuais exigem autenticação e permissão de edição do evento. Conforme a regra de acesso da aplicação, isso inclui proprietário/editor autorizado e, para as rotas de emissão manual, moderador ou administrador.

### Ministrante ou convidado apresentador

`POST /api/events/{eventId}/instructor-certificate/manual`

O corpo deve indicar **uma** destas opções:

- `instructorId`: um registro de ministrante confirmado que pertença ao evento; ou
- `activityId` e `activityGuestId`: um vínculo de convidado à atividade do evento, com papel `PRESENTER` e perfil confirmado.

No caso de ministrante com conta, a carga horária é a soma das atividades do evento em que essa conta tem vínculos confirmados. Sem conta, usa-se a carga da atividade daquele registro. Para convidado apresentador, usa-se a carga da atividade indicada. A emissão aceita convidado sem conta.

Esta emissão manual não cria código de validação. No template, os marcadores de validação recebem o texto `Emissão manual` e uma URL vazia.

### Integrante da equipe

`POST /api/events/{eventId}/team-certificate/manual/{recipientId}`

A equipe emite individualmente para o proprietário ou um editor ativo. Exige evento encerrado, não cancelado, e carga horária de equipe configurada. Usa o template `EDITOR` e os dados reais do destinatário. Quando o template inclui os marcadores de validação, é emitido ou reutilizado um código do tipo `EDITOR`.

### Certificado personalizado

- `POST /api/events/{eventId}/certificate/manual/preview`: gera uma prévia com o fundo atual e as caixas e valores enviados; não persiste nem emite um certificado definitivo.
- `POST /api/events/{eventId}/certificate/manual`: gera a imagem definitiva, somente após o encerramento do evento e se ele não estiver cancelado.

O corpo fornece `boxes` e um mapa `variables`. Essa emissão não está ligada automaticamente a uma inscrição, ministrante, convidado ou editor e não cria código de validação. A equipe é responsável por enviar os valores corretos. O conteúdo recebido não é salvo.

**Não há uma rota dedicada para a equipe emitir individualmente o certificado padrão de participante.** Para participante, o fluxo implementado é a autoemissão após cumprir os requisitos; o certificado personalizado não verifica nem registra o vínculo com uma inscrição.

## 5. Templates e aparência

O fundo é compartilhado por todos os tipos de certificado do evento. Se não houver fundo próprio, usa-se `templates/certificado-default.png`. Os templates de caixas são separados em:

| Tipo | Escopo |
|---|---|
| `PARTICIPANT` | Certificado geral de participante do evento |
| `INSTRUCTOR` | Ministrantes e convidados apresentadores |
| `EDITOR` | Proprietário e editores da equipe |
| Atividade | Um único template compartilhado por todas as atividades do evento |

Quando um template de caixas não foi salvo, a aplicação usa um padrão. A prévia usa o fundo e as caixas configurados, mas preenche os marcadores com valores de exemplo; ela não valida a elegibilidade de um destinatário real.

As caixas definem texto, posição normalizada (`x`, `y`), largura, tamanho e cor da fonte e alinhamento. Cada template aceita de 1 a 30 caixas. Os marcadores encontrados no texto são extraídos e validados contra as variáveis permitidas para o contexto.

## 6. Variáveis disponíveis

Os nomes devem ser usados exatamente como mostrados, incluindo chaves e acentos:

| Contexto | Variáveis |
|---|---|
| Participante | `{participante.nome}`, `{evento.carga_horaria}`, `{evento.title}`, `{evento.local}`, `{evento.organizacao}`, `{validacao.codigo}`, `{validacao.url}` |
| Atividade | `{participante.nome}`, `{evento.title}`, `{evento.local}`, `{evento.organizacao}`, `{evento.carga_horaria}`, `{atividade.nome}`, `{atividade.local}`, `{atividade.carga_horaria}`, `{validacao.codigo}`, `{validacao.url}` |
| Ministrante | `{ministrante.nome}`, `{ministrante.carga_horaria}`, `{evento.title}`, `{evento.local}`, `{evento.organizacao}`, `{validacao.codigo}`, `{validacao.url}`; o contexto também aceita `{participante.nome}` e `{evento.carga_horaria}` |
| Equipe | `{equipe.nome}`, `{equipe.carga_horaria}`, `{evento.title}`, `{evento.local}`, `{evento.organizacao}`, `{validacao.codigo}`, `{validacao.url}`; o contexto também aceita `{participante.nome}` e `{evento.carga_horaria}` |

`atividade.carga_horaria` contém apenas o número de horas. Cargas são formatadas sem zeros decimais finais. Local e organização ausentes são apresentados como `Não consta`. Marcadores de validação só geram código quando são usados no template, salvo a emissão manual de ministrante, que sempre usa `Emissão manual` e URL vazia.

## 7. Código e consulta de validação

`GET /api/events/certificates/validate/{code}` é público. O código tem oito caracteres, é normalizado para maiúsculas e espaços externos são removidos. A resposta informa pessoa, evento, tipo, carga horária e, para atividade, o título da atividade. Não encontrar um código válido resulta em `404`.

Os códigos são registros separados das imagens e são criados/reutilizados para participante de evento, participante de atividade, ministrante e equipe quando o template solicita os campos de validação. As emissões manuais personalizadas não criam código; a emissão manual de ministrante também não. A emissão manual de equipe cria/reutiliza código se o template `EDITOR` contiver os marcadores.

## 8. Rotas de configuração e consulta

Todas as rotas abaixo exigem JWT. Consultas e prévias exigem acesso de visualização ao evento; alterações exigem permissão de edição.

| Método e rota | Uso |
|---|---|
| `GET /api/events/{eventId}/certificate-box-template?type=PARTICIPANT\|INSTRUCTOR\|EDITOR` | Consulta caixas e variáveis de um template de evento; `type` padrão é `PARTICIPANT`. |
| `POST /api/events/{eventId}/certificate-box-template?type=PARTICIPANT\|INSTRUCTOR\|EDITOR` | Salva as caixas para o tipo indicado. |
| `GET /api/events/{eventId}/activity-certificate-box-template` | Consulta o template compartilhado pelas atividades. |
| `POST /api/events/{eventId}/activity-certificate-box-template` | Salva o template compartilhado pelas atividades. |
| `GET /api/events/{eventId}/certificate-cover-template` | Baixa o fundo atual ou o padrão. |
| `POST /api/events/{eventId}/certificate-cover-template` | Envia/substitui o fundo do evento (PNG, JPEG ou WebP). |
| `DELETE /api/events/{eventId}/certificate-cover-template` | Remove o fundo próprio. |
| `GET /api/events/{eventId}/certificate-preview?type=PARTICIPANT\|INSTRUCTOR\|EDITOR` | Prévia de um template do evento com valores de exemplo. |
| `GET /api/events/{eventId}/activity-certificate-preview` | Prévia do template de atividade com valores de exemplo. |
| `GET /api/events/certificate-variables?type=PARTICIPANT\|ACTIVITY\|INSTRUCTOR\|EDITOR` | Lista as variáveis válidas para o contexto; `type` padrão é `PARTICIPANT`. |

O upload aceita imagem PNG, JPEG ou WebP. As dimensões da caixa são proporcionais ao fundo usado na geração.

## 9. Relação de rotas de emissão

| Método e rota | Destinatário |
|---|---|
| `GET /api/events/{eventId}/certificate` | Participante autenticado do evento |
| `GET /api/events/activities/{activityId}/certificate` | Participante autenticado da atividade |
| `GET /api/events/{eventId}/instructors/{instructorId}/certificate` | Ministrante com conta vinculada |
| `GET /api/events/{eventId}/activities/{activityId}/guests/{activityGuestId}/instructor-certificate` | Convidado apresentador com conta vinculada |
| `GET /api/events/{eventId}/team-certificate` | Proprietário/editor ativo autenticado |
| `POST /api/events/{eventId}/instructor-certificate/manual` | Ministrante ou convidado apresentador indicado pela equipe |
| `POST /api/events/{eventId}/team-certificate/manual/{recipientId}` | Proprietário/editor ativo indicado pela equipe |
| `POST /api/events/{eventId}/certificate/manual/preview` | Prévia personalizada enviada pela equipe |
| `POST /api/events/{eventId}/certificate/manual` | Certificado personalizado enviado pela equipe |
| `GET /api/events/certificates/validate/{code}` | Consulta pública do código impresso |

Veja também a [matriz geral de acesso às rotas](access-control-matrix.md). A documentação dos vínculos de convidados e ministrantes está em [Convidados e ministrantes](convidados-e-ministrantes.md).
