# Projeto para o Curso de Ciência da Computação

Plataforma web dedicada ao Curso de Ciência da Computação do Instituto
Federal do Maranhão (IFMA). Atualmente, a instituição não possui sites
específicos para cada curso, concentrando as informações apenas no
portal geral do campus.

O projeto centraliza informações institucionais, divulga eventos e
atividades, fortalece a comunidade acadêmica e promove a interação
entre alunos, professores e o público externo em relação às áreas de
atuação da computação.

Este repositório contém o **backend** do projeto, desenvolvido em
**Java 25 LTS** com **Spring Boot 4**.

## Sumário

- [Sobre o Projeto](#projeto-para-o-curso-de-ciência-da-computação)
- [Execução Local](#execução-local)
    - [Via Docker Compose (Recomendado)](#via-docker-compose-recomendado)
    - [Execução Local (Shell)](#via-shell)
- [Documentação da API](#documentação-da-api)
- [Infraestrutura em Produção](#infraestrutura-em-produção)

## Execução Local

### Via Docker Compose (Recomendado)

Esta é a maneira mais rápida e prática de rodar o projeto em
desenvolvimento. Todos os serviços necessários já vêm
orquestrados e configurados nos arquivos `docker-compose.yml`
e `docker-compose.dev.yml`.
Crie um arquivo `.env.dev` na raiz do projeto com as variáveis abaixo:

```dotenv
SPRING_PROFILES_ACTIVE=dev

POSTGRES_URL=jdbc:postgresql://postgres-db:5432/ccomp-db
POSTGRES_USER=user
POSTGRES_PASSWORD=password
POSTGRES_DB=ccomp-db
POSTGRES_DRIVER=org.postgresql.Driver
POSTGRES_DIALECT=org.hibernate.dialect.PostgreSQLDialect

OTEL_OTLP_ENDPOINT=http://grafana-lgtm:4318

PUBLIC_KEY=file:/app/keys/public.key
PRIVITE_KEY=file:/app/keys/private.key

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
USERNAME=seu@email.com
MAIL_PASSWORD=senha
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

STORAGE_ENDPOINT=http://minio:9000
STORAGE_ACCESS_KEY=minioadmin
STORAGE_SECRET_KEY=minioadmin
STORAGE_BUCKET=images
STORAGE_REGION=us-east-1
```
- Nota: Como o ambiente de desenvolvimento roda de forma 
isolada no Docker, o serviço `keygen` (definido no `docker-compose.dev.yml`)
se encarrega de gerar as chaves públicas e privadas.

Em seguida, suba os containers:

```shell
docker compose -f docker-compose.dev.yml --profile all up --build
```

### Via Shell

Na situação em que deseja apenas executar a aplicação, é necessario
alterar as seguintes informações do `.env.dev` ao rodar 
localmente fora do docker.

```dotenv
# Carrega as variaveis de sua pasta na raiz do projeto
PUBLIC_KEY=file:./keys/public.key
PRIVITE_KEY=file:./keys/private.key

# Caso não queira criar um bacno para persistir os dados.
DB_URL=jdbc:h2:mem:ccomp-db;DB_CLOSE_DELAY=-1
DB_USER=sa
DB_PASS=
DB_DRIVER=org.h2.Driver
DB_DIALECT=org.hibernate.dialect.H2Dialect
```

- Obs: Ao optar por este caminho local sem Docker, a aplicação 
não gerará as chaves criptográficas de forma autônoma. Será 
obrigatório criar o par de chaves pública e privada manualmente 
na pasta `./keys`, usando o método de sua preferência, antes de 
iniciar o projeto.

O perfil default `dev` vai ser usado e as configurações do
`application.properties` serão usadas. A aplicação vai carregar
o `.env.dev` na execução.

Por fim, execute o comando no terminal na raiz do projeto:
```shell
./mvnw spring-boot:run 
```

## Documentação da API 

Com a aplicação em execução, a documentação interativa (Swagger UI)
fica disponível em:

```
http://localhost:8080/swagger-ui.html
```

## Infraestrutura em Produção

Em produção, apenas o backend (via proxy reverso) é exposto
publicamente. Os demais serviços (banco de dados, storage,
observabilidade) ficam acessíveis apenas na rede interna do
host, com bind em `127.0.0.1`.

### Executando em produção

Antes do primeiro `up`, as chaves RSA usadas para assinatura dos
JWTs precisam existir em `./keys`. Elas **não são geradas
automaticamente** em produção. Sequencia de comandos para
gerar as chaves:

```shell
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/private.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private.key -out keys/public.key
chmod 600 keys/private.key
chmod 644 keys/public.key
```
Também é necessário um arquivo `.env` na raiz do projeto,
na raiz do projeto, contendo as configurações do perfil e 
as credenciais de infraestrutura orquestradas 
pelo `docker-compose.prod.yml`:

```dotenv
SPRING_PROFILES_ACTIVE=prod

# Dados da aplicação em produção
FRONTEND_PASSWORD_RESET_URL=localhost:4321/reset-password
PUBLIC_KEY=file:./keys/public.key
PRIVITE_KEY=file:./keys/private.key

POSTGRES_URL=jdbc:postgresql://postgres-db:5432/ccomp-db
POSTGRES_USER=user # usuário rela
POSTGRES_PASSWORD=password # Senha real
POSTGRES_DB=ccomp-db
POSTGRES_DRIVER=org.postgresql.Driver
POSTGRES_DIALECT=org.hibernate.dialect.PostgreSQLDialect

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
USERNAME=seu@email.com
MAIL_PASSWORD=senha
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

# Só descomente se for rodar a aplicação no docker
OTEL_OTLP_ENDPOINT=http://grafana-lgtm:4318

# Ajustar para o verdadeiro em produção
STORAGE_ENDPOINT=http://minio:9000
STORAGE_USER=minioadmin
STORAGE_PASSWORD=minioadmin # Senha real
STORAGE_BUCKET=images
STORAGE_REGION=us-east-1
```

Com as chaves e o `.env` criados, o ambiente é subido com:

```shell
docker compose -f docker-compose.prod.yml --profile all up --build
```

- (Adicione `-d` ao final do comando para rodar em background/daemon mode).

O `--profile all` garante que todos os serviços 
(`proxy`, `ccomp-backend`, `postgres-db`, `minio`, 
`minio-setup`, `grafana-lgtm`) sejam inicializados 
corretamente conforme mapeado na rede.

### Acessando serviços internos localmente (túnel SSH)

Para acessar ferramentas administrativas ou o
banco de dados diretamente em produção, é necessário estabelecer
um túnel SSH até o servidor, com cada um deles. Para o Grafana é:

```shell
ssh -L 3000:127.0.0.1:3000 usuario@<host>
```

O mesmo vale para o Postgres, permitindo conectar via
ferramentas como o pgAdmin apontando para `127.0.0.1:5432`:

```shell
ssh -L 5432:127.0.0.1:5432 usuario@<host>
```