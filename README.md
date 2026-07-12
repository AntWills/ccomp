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

## Como Executar

A forma recomendada é rodar o projeto via Docker, com os serviços
orquestrados pelos arquivos `docker-compose.yml` e
`docker-compose.dev.yml`.

Crie um arquivo `.env` na raiz do projeto com as variáveis abaixo:

```dotenv
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:postgresql://postgres-db:5432/ccomp-db
#DB_URL=jdbc:postgresql://localhost:5432/ccomp-db
DB_USER=user
DB_PASS=password
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect

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

Em seguida, suba os containers:

```shell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

## Documentação da API

Com a aplicação em execução, a documentação interativa (Swagger UI)
fica disponível em:

```
http://localhost:8080/swagger-ui.html
```