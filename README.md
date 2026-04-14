# Webhook Listener API

![Java](https://img.shields.io/badge/Java-17+-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-6DB33F?logo=springboot&logoColor=white)
![Spring Web MVC](https://img.shields.io/badge/Spring_Web_MVC-Framework-6DB33F?logo=spring&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-Persistence-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-Annotations-BC4521?logo=java&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?logo=swagger&logoColor=black)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white)

API REST para receber webhooks via HTTP, validar assinatura e payload, persistir os eventos no banco e disponibilizar uma consulta simples dos eventos recebidos.

## O que este projeto e

Este projeto e um webhook listener para eventos externos.

Na versao atual, ele recebe eventos GitHub em `POST /webhooks/github`, valida o header `X-Hub-Signature-256` com HMAC SHA-256 usando o body bruto da requisicao, impede duplicidade pelo `externalEventId`, salva o evento no PostgreSQL e permite consultar os eventos recebidos em `GET /webhooks/events`.

## Fluxo do webhook

1. O emissor envia um `POST` para `/webhooks/github`
2. A API valida o header `X-Hub-Signature-256`
3. A API valida o JSON recebido
4. A API verifica se o evento ja foi recebido
5. A API salva o evento com `source=github` e `status=RECEIVED`
6. A API retorna a resposta HTTP apropriada

## Como rodar

### Pre-requisitos

- Java 17 ou superior
- PostgreSQL em execucao

### Criar banco

```sql
CREATE DATABASE webhook_listener;
```

### Configurar variaveis de ambiente

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

### Subir a aplicacao

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
./mvnw spring-boot:run
```

Aplicacao disponivel em:

```text
http://localhost:8080
```

## Endpoints

### POST `/webhooks/github`

Recebe um webhook GitHub.

Headers:

```http
X-Hub-Signature-256: sha256=<hex>
Content-Type: application/json
```

Payload:

```json
{
  "id": "evt-123",
  "action": "opened",
  "repository": "owner/repo",
  "timestamp": "2026-04-13T18:00:00Z"
}
```

Regras:

- `source` nao vem no payload; ele e definido internamente como `github`
- o status inicial do evento salvo e `RECEIVED`
- o header `X-Hub-Signature-256` deve seguir o formato `sha256=<hex>`

Respostas:

- `201 Created` evento salvo com sucesso
- `400 Bad Request` payload invalido ou header ausente/malformado
- `401 Unauthorized` assinatura invalida
- `409 Conflict` evento duplicado

### GET `/webhooks/events`

Retorna a lista de eventos recebidos, ordenada por `receivedAt` decrescente.

## Exemplo de chamada

```http
POST /webhooks/github HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-Hub-Signature-256: sha256=...

{
  "id": "evt-123",
  "action": "opened",
  "repository": "owner/repo",
  "timestamp": "2026-04-13T18:00:00Z"
}
```

## Documentacao

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

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
|-- controller
|-- dto
|-- entity
|-- exception
|-- repository
`-- service
```

## Testes

```powershell
.\mvnw.cmd test
```

Cobre fluxo valido, assinatura invalida, payload invalido, duplicidade, listagem e unicidade no repositorio.
