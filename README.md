# Webhook Listener API

![Java](https://img.shields.io/badge/Java-17+-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![Spring Web MVC](https://img.shields.io/badge/Spring_Web_MVC-Framework-6DB33F?logo=spring&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-Persistence-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-Annotations-BC4521?logo=java&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?logo=swagger&logoColor=black)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white)

API REST em Java com Spring Boot para receber webhooks via HTTP, validar assinatura e payload, persistir os eventos no banco e disponibilizar uma listagem simples dos eventos recebidos.

## O que o projeto faz

- Recebe eventos externos em `POST /webhooks/github`
- Valida o header `X-Signature` com HMAC SHA-256 usando o body bruto da requisição
- Valida os campos obrigatorios do payload
- Rejeita assinatura invalida
- Rejeita evento duplicado pelo `externalEventId`
- Salva o evento no banco PostgreSQL
- Lista eventos recebidos em `GET /webhooks/events`
- Exponibiliza documentacao OpenAPI/Swagger

## Stack

- Java 17+
- Spring Boot
- Spring Web MVC
- Spring Validation
- Spring Data JPA
- PostgreSQL
- Lombok
- Swagger / OpenAPI

## Estrutura

```text
src/main/java/com/felipe/webhook_listener_api
├── controller
├── dto
├── entity
├── exception
├── repository
└── service
```

## Regras principais

- O endpoint inicial suportado e `POST /webhooks/github`
- O campo `source` nao vem no payload; ele e definido internamente como `github`
- O status inicial do evento salvo e `RECEIVED`
- O header `X-Signature` deve seguir o formato `sha256=<hex>`
- O payload deve conter:

```json
{
  "id": "evt-123",
  "action": "opened",
  "repository": "owner/repo",
  "timestamp": "2026-04-13T18:00:00Z"
}
```

## Como rodar

### 1. Pre-requisitos

- Java 17 ou superior
- PostgreSQL em execucao

### 2. Criar banco

Exemplo:

```sql
CREATE DATABASE webhook_listener;
```

### 3. Configurar variaveis de ambiente

O projeto usa estas propriedades:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `WEBHOOK_GITHUB_SECRET`
- `JPA_DDL_AUTO` opcional, padrao `update`

Exemplo no PowerShell:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/webhook_listener"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="postgres"
$env:WEBHOOK_GITHUB_SECRET="meu-segredo"
```

### 4. Subir a aplicacao

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
./mvnw spring-boot:run
```

A API sobe por padrao em:

```text
http://localhost:8080
```

## Documentacao

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Endpoints

### POST `/webhooks/github`

Recebe um webhook GitHub.

Headers:

```http
X-Signature: sha256=<hex>
Content-Type: application/json
```

Respostas:

- `201 Created` evento salvo com sucesso
- `400 Bad Request` payload invalido ou header malformado/ausente
- `401 Unauthorized` assinatura invalida
- `409 Conflict` evento duplicado

### GET `/webhooks/events`

Retorna a lista de eventos recebidos, ordenada por `receivedAt` decrescente.

## Exemplo de chamada

```http
POST /webhooks/github HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-Signature: sha256=...

{
  "id": "evt-123",
  "action": "opened",
  "repository": "owner/repo",
  "timestamp": "2026-04-13T18:00:00Z"
}
```

## Testes

```powershell
.\mvnw.cmd clean test
```

Cobre fluxo valido, assinatura invalida, payload invalido, duplicidade, listagem e unicidade no repositorio.
