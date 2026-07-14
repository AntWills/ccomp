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
- [Como Executar](#como-executar)
    - [Via Docker Compose (Recomendado)](#via-docker-compose-recomendado)
    - [Execução Local (IDE)](#execução-local-ide)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)

## Como Executar

### Via Docker Compose (Recomendado)

Esta é a maneira mais rápida e prática de rodar o projeto em 
desenvolvimento. Todos os serviços necessários já vêm 
orquestrados e configurados nos arquivos `docker-compose.yml` 
e `docker-compose.dev.yml`.

Crie um arquivo `.env.dev` na raiz do projeto com as variáveis abaixo:

```dotenv
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:postgresql://postgres-db:5432/ccomp-db
DB_USER=user
DB_PASS=password
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect

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
docker compose -f docker-compose.dev.yml up
```

### Execução Local (IDE)

Na situação em que deseja apenas executar a aplicação, recomenda-se
configurar a IDE de sua preferencia para carregar o arquivo `.env.dev`
ao rodar localmente com as alterações a seguir.

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
- 
## Documentação da API (Swagger)

Com a aplicação em execução, a documentação interativa (Swagger UI)
fica disponível em:

```
http://localhost:8080/swagger-ui.html
```