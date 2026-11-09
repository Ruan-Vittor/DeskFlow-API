# DeskFlow API

API REST para gerenciamento de chamados de suporte técnico, desenvolvida com **Java 21**, **Spring Boot**, **Spring Security**, **JWT**, **PostgreSQL**, **Flyway** e **Swagger/OpenAPI**.

O projeto foi construído como uma aplicação de portfólio backend, com foco em regras de negócio centralizadas na camada de serviço, autorização por perfil, validação de dados, respostas padronizadas e testes automatizados.

---

## Sumário

- [Visão geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Perfis e permissões](#perfis-e-permissões)
- [Regras de negócio](#regras-de-negócio)
- [Requisitos](#requisitos)
- [Configuração do banco](#configuração-do-banco)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Como executar](#como-executar)
- [Documentação Swagger](#documentação-swagger)
- [Autenticação JWT](#autenticação-jwt)
- [Endpoints](#endpoints)
- [Exemplos de uso](#exemplos-de-uso)
- [Paginação, ordenação e filtros](#paginação-ordenação-e-filtros)
- [Formato das respostas](#formato-das-respostas)
- [Testes](#testes)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Decisões técnicas](#decisões-técnicas)

---

## Visão geral

O DeskFlow permite que usuários abram chamados de suporte e acompanhem o atendimento. Técnicos podem assumir chamados, alterar prioridade, mover o status e comentar. Administradores gerenciam usuários e todos os chamados.

Principais características:

- Cadastro público sempre cria usuário com perfil `REQUESTER`.
- Login com e-mail e senha, retornando token JWT.
- Senhas armazenadas com hash BCrypt.
- Autorização por perfil e por vínculo com o chamado.
- Fluxo de status com transições válidas.
- Chamados encerrados não podem ser editados, atribuídos, ter prioridade alterada ou receber comentários.
- Persistência em PostgreSQL com schema versionado por Flyway.
- Testes de integração com H2 em modo PostgreSQL, sem depender de banco externo.
- Documentação interativa via Swagger UI.

---

## Funcionalidades

### Autenticação e usuários

- Cadastro público de usuário (`REQUESTER`).
- Login com JWT.
- Consulta e atualização do próprio perfil.
- Alteração de senha mediante confirmação da senha atual.
- Listagem de usuários e criação de usuários com perfil específico por administradores.
- Criação opcional de administrador inicial por variáveis de ambiente.

### Chamados

- Abertura de chamados com título, descrição, categoria e prioridade.
- Consulta por ID com controle de acesso.
- Listagem paginada com filtros por status, prioridade e categoria.
- Atualização de título, descrição, categoria e prioridade.
- Atribuição a técnico.
- Alteração de status com validação de transição.
- Atalhos para encerrar e reabrir chamados.
- Alteração isolada de prioridade.
- Comentários por chamado.
- `closedAt` preenchido automaticamente ao encerrar e limpo ao reabrir.

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem e versão de compilação (`--release 21`) |
| Spring Boot 4.1.2 | Framework da aplicação |
| Spring Web MVC | API REST |
| Spring Data JPA / Hibernate | Persistência |
| Spring Security | Autenticação e autorização |
| JJWT 0.13.0 | Geração e validação de tokens JWT |
| Bean Validation | Validação de DTOs |
| PostgreSQL | Banco de dados de produção |
| Flyway | Versionamento do schema |
| H2 | Banco em memória para testes |
| Springdoc OpenAPI 3.2.0 | Swagger UI e especificação OpenAPI |
| JUnit 5 + Mockito + AssertJ | Testes automatizados |
| Maven | Build e gerenciamento de dependências |

---

## Arquitetura

A aplicação segue uma organização em camadas:

```text
Controller  ->  Service  ->  Repository  ->  PostgreSQL
     |             |
     |             +--> Policies (regras de acesso e transição de status)
     |
     +--> DTOs de entrada/saída
```

- **Controllers**: expõem os endpoints, recebem DTOs validados e delegam para os serviços.
- **Services**: concentram regras de negócio, transações e autorização contextual.
- **Policies**: isolam regras reutilizáveis de acesso e transição de status.
- **Repositories**: consultas JPA com escopo por perfil.
- **DTOs**: records para entrada e saída, evitando exposição direta das entidades.
- **Mappers**: convertem entidades em respostas.
- **Exception Handler global**: padroniza erros HTTP.

---

## Perfis e permissões

| Perfil | Descrição |
|---|---|
| `ADMIN` | Gerencia usuários e todos os chamados. Pode criar usuários com qualquer perfil. |
| `TECHNICIAN` | Visualiza chamados sem técnico, atribuídos a si e chamados dos quais é solicitante. Pode assumir chamados e atendê-los. |
| `REQUESTER` | Visualiza apenas os próprios chamados. Pode abrir, acompanhar, comentar e executar transições permitidas. |

### Visibilidade de chamados

| Ação | `ADMIN` | `TECHNICIAN` | `REQUESTER` |
|---|---|---|---|
| Ver qualquer chamado | Sim | Não | Não |
| Ver chamado sem técnico | Sim | Sim | Somente se for o solicitante |
| Ver chamado atribuído a outro técnico | Sim | Não | Somente se for o solicitante |
| Ver chamado atribuído a si | Sim | Sim | Somente se for o solicitante |
| Editar conteúdo do chamado | Sim, se não estiver encerrado | Não | Sim, se for o solicitante e não estiver encerrado |
| Atribuir chamado | Sim, para qualquer técnico ativo | Sim, somente para si | Não |
| Alterar prioridade | Sim, se não estiver encerrado | Sim, se for o técnico responsável | Não |
| Alterar status | Qualquer transição válida | Qualquer transição válida, se for o técnico responsável | Somente transições específicas |
| Comentar | Sim, se não estiver encerrado | Sim, se tiver acesso e não estiver encerrado | Sim, se tiver acesso e não estiver encerrado |

> O cadastro público ignora qualquer tentativa de informar perfil. Usuários `ADMIN` e `TECHNICIAN` só podem ser criados por um administrador em `POST /api/users`.

---

## Regras de negócio

### Categorias

`HARDWARE`, `SOFTWARE`, `NETWORK`, `ACCESS`, `OTHER`.

### Prioridades

`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.

### Status e transições

| Status atual | Pode ir para |
|---|---|
| `OPEN` | `IN_PROGRESS`, `CLOSED` |
| `IN_PROGRESS` | `WAITING_FOR_USER`, `RESOLVED`, `CLOSED` |
| `WAITING_FOR_USER` | `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `RESOLVED` | `CLOSED`, `OPEN` |
| `CLOSED` | `OPEN` |

Além da transição válida, o perfil precisa ter permissão:

| Transição permitida ao `REQUESTER` | |
|---|---|
| `OPEN` -> `CLOSED` | Encerrar o próprio chamado |
| `WAITING_FOR_USER` -> `IN_PROGRESS` | Responder ao técnico |
| `RESOLVED` -> `OPEN` | Reabrir chamado resolvido |
| `CLOSED` -> `OPEN` | Reabrir chamado encerrado |

`ADMIN` e o técnico responsável podem executar qualquer transição válida da tabela geral.

### Outras regras

- Todo chamado nasce com status `OPEN` e sem técnico.
- Apenas usuários com perfil `TECHNICIAN` e ativos podem ser atribuídos.
- Técnicos só podem atribuir chamados a si mesmos.
- Chamados encerrados não podem ser editados, atribuídos, ter prioridade alterada ou receber comentários.
- Apenas o solicitante proprietário ou um `ADMIN` pode editar o conteúdo do chamado.
- A descrição deve ter entre 10 e 4.000 caracteres.
- Comentários devem ter entre 1 e 2.000 caracteres.
- E-mails são normalizados para minúsculas e não podem se repetir.
- Senhas devem ter entre 8 e 72 caracteres.
- Campos de ordenação são validados por lista permitida, e o tamanho máximo de página é 100.

---

## Requisitos

- **Java 21 ou superior**
- **Maven 3.9+**
- **PostgreSQL 13+**

Não é necessário Docker.

> O projeto compila com `--release 21` e utiliza Spring Boot 4.1.2. O build e os testes foram executados com JDK 27 e Maven 3.10.

---

## Configuração do banco

Crie o banco e, opcionalmente, um usuário dedicado:

```sql
CREATE USER deskflow WITH PASSWORD 'sua_senha_forte';
CREATE DATABASE deskflow OWNER deskflow;
```

Ou, usando `psql` como superusuário:

```bash
psql -U postgres -c "CREATE USER deskflow WITH PASSWORD 'sua_senha_forte';"
psql -U postgres -c "CREATE DATABASE deskflow OWNER deskflow;"
```

O Flyway cria automaticamente as tabelas `users`, `tickets`, `comments`, índices e constraints na primeira inicialização.

A migration fica em:

```text
src/main/resources/db/migration/V1__create_tables.sql
```

### Cluster local dedicado no Windows (opcional)

Para desenvolvimento sem depender das credenciais do PostgreSQL já instalado, o projeto inclui scripts que criam um cluster isolado em `.postgres/` na porta `5433`. Esse cluster usa autenticação `scram-sha-256` e não altera o serviço existente na porta `5432`.

Pré-requisito: PostgreSQL instalado em `C:\Program Files\PostgreSQL\18\bin`. Se o caminho for diferente, informe-o com o parâmetro `-PgBin`.

1. Crie o `.env` com `DB_URL` apontando para a porta `5433`, `DB_USERNAME=deskflow` e uma `DB_PASSWORD` forte.
2. Execute o setup uma única vez:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-local-db.ps1
```

3. Inicie ou pare o cluster quando necessário:

```powershell
.\scripts\start-local-db.ps1
.\scripts\stop-local-db.ps1
```

Para iniciar o banco e a API em um único comando:

```powershell
.\scripts\run-dev.ps1
```

O setup é idempotente: cria o cluster se necessário, garante o banco `deskflow`, define a senha e valida a conexão autenticada. O diretório `.postgres/` está no `.gitignore` e não deve ser versionado.

---

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `DB_URL` | Não | `jdbc:postgresql://localhost:5432/deskflow` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | Não | `deskflow` | Usuário do banco |
| `DB_PASSWORD` | Não | vazio | Senha do banco |
| `JWT_SECRET` | **Sim** | — | Segredo HMAC com no mínimo 32 bytes |
| `JWT_EXPIRATION_MINUTES` | Não | `60` | Validade do token em minutos |
| `SERVER_PORT` | Não | `8080` | Porta HTTP da API |
| `CORS_ALLOWED_ORIGINS` | Não | `http://localhost:3000,http://localhost:5173` | Origens permitidas, separadas por vírgula |
| `BOOTSTRAP_ADMIN_NAME` | Não | `Administrador` | Nome do admin inicial |
| `BOOTSTRAP_ADMIN_EMAIL` | Não | vazio | E-mail do admin inicial; vazio desativa o bootstrap |
| `BOOTSTRAP_ADMIN_PASSWORD` | Não | vazio | Senha do admin inicial; vazio desativa o bootstrap |

### Arquivo `.env`

A aplicação tenta carregar automaticamente um arquivo `.env` no diretório de execução:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

Crie o arquivo a partir do modelo:

```bash
cp .env.example .env
```

No PowerShell:

```powershell
Copy-Item .env.example .env
```

Exemplo de `.env`:

```properties
DB_URL=jdbc:postgresql://localhost:5432/deskflow
DB_USERNAME=deskflow
DB_PASSWORD=sua_senha_forte

JWT_SECRET=uma-chave-aleatoria-com-pelo-menos-32-caracteres
JWT_EXPIRATION_MINUTES=60

SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

BOOTSTRAP_ADMIN_NAME=Administrador
BOOTSTRAP_ADMIN_EMAIL=admin@deskflow.local
BOOTSTRAP_ADMIN_PASSWORD=uma-senha-forte
```

> Gere um segredo seguro, por exemplo: `openssl rand -base64 48`. Nunca versione o arquivo `.env` nem o segredo JWT.

O administrador inicial só é criado se `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` estiverem preenchidos. Se o e-mail já existir, nada é alterado.

---

## Como executar

### 1. Instalar dependências e compilar

```bash
mvn clean compile
```

### 2. Executar em modo de desenvolvimento

Antes de iniciar a API, garanta que o PostgreSQL está em execução. Se estiver usando o cluster local dedicado, inicie o banco:

```powershell
.\scripts\start-local-db.ps1
```

Depois:

```bash
mvn spring-boot:run
```

Alternativamente, inicie banco e API juntos:

```powershell
.\scripts\run-dev.ps1
```

### 3. Empacotar e executar o JAR

```bash
mvn clean package
java -jar target/deskflow-1.0.0.jar
```

A API ficará disponível em:

```text
http://localhost:8080
```

### 4. Verificar saúde da aplicação

A aplicação não expõe endpoint de health por padrão. Para confirmar que subiu, acesse o Swagger UI ou faça uma chamada de login/cadastro.

### Erros comuns

#### `JWT_SECRET deve ser configurado com no mínimo 32 bytes`

Causa: a variável `JWT_SECRET` não foi definida e o arquivo `.env` não existe no diretório de onde a aplicação foi iniciada.

Solução:

```powershell
Copy-Item .env.example .env
# Edite o .env e defina um JWT_SECRET com pelo menos 32 caracteres.
```

O `mvn spring-boot:run` deve ser executado na raiz do projeto para que o `.env` seja carregado.

#### `autenticação do tipo senha falhou` ou `password authentication failed`

Causa: `DB_URL`, `DB_USERNAME` ou `DB_PASSWORD` incorretos.

- Confira os valores no `.env`.
- Se estiver usando o cluster local, confirme que ele está rodando com `.\scripts\start-local-db.ps1`.
- O cluster local usa a porta `5433`; o PostgreSQL instalado como serviço normalmente usa `5432`.

#### `Connection refused` ao conectar no banco

Causa: o PostgreSQL não está em execução ou a porta está incorreta.

```powershell
.\scripts\start-local-db.ps1
```

---

## Documentação Swagger

Com a aplicação em execução:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

Para testar endpoints protegidos:

1. Execute `POST /api/auth/login` ou `POST /api/auth/register`.
2. Copie o valor de `data.token`.
3. Clique em **Authorize** no Swagger UI.
4. Informe o token. O esquema é `Bearer`; dependendo da interface, informe apenas o token ou `Bearer <token>`.

---

## Autenticação JWT

O token é assinado com HMAC usando `JWT_SECRET` e contém:

- `sub`: e-mail do usuário;
- `uid`: ID do usuário;
- `role`: perfil;
- `iat` e `exp`: emissão e expiração.

Em cada requisição autenticada, o usuário é recarregado do banco para garantir que o perfil e o status ativo estejam atualizados.

Envie o token no cabeçalho:

```http
Authorization: Bearer <token>
```

### Cadastro público

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Souza",
    "email": "maria@example.com",
    "password": "senha-forte-123"
  }'
```

Resposta `201 Created`:

```json
{
  "success": true,
  "message": "Usuário cadastrado com sucesso.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "name": "Maria Souza",
      "email": "maria@example.com",
      "role": "REQUESTER",
      "active": true,
      "createdAt": "2026-11-08T17:42:00Z"
    }
  },
  "timestamp": "2026-11-08T17:42:00Z"
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria@example.com",
    "password": "senha-forte-123"
  }'
```

Resposta `200 OK` com a mesma estrutura de `AuthResponse`.

### Usar o token

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## Endpoints

### Autenticação

| Método | Caminho | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Cadastra um `REQUESTER` e retorna JWT |
| `POST` | `/api/auth/login` | Público | Autentica e retorna JWT |

### Usuários

| Método | Caminho | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/users` | `ADMIN` | Lista usuários com paginação |
| `POST` | `/api/users` | `ADMIN` | Cria usuário com perfil específico |
| `GET` | `/api/users/me` | Autenticado | Consulta o próprio perfil |
| `PUT` | `/api/users/me` | Autenticado | Atualiza nome e, opcionalmente, senha |

### Chamados

| Método | Caminho | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/tickets` | Autenticado | Abre um chamado |
| `GET` | `/api/tickets` | Autenticado | Lista chamados conforme o perfil |
| `GET` | `/api/tickets/{id}` | Com acesso ao chamado | Consulta um chamado |
| `PUT` | `/api/tickets/{id}` | `ADMIN` ou solicitante | Atualiza conteúdo do chamado |
| `PATCH` | `/api/tickets/{id}/status` | Conforme regras | Altera o status |
| `PATCH` | `/api/tickets/{id}/assign` | `ADMIN` ou `TECHNICIAN` | Atribui o chamado |
| `PATCH` | `/api/tickets/{id}/priority` | `ADMIN` ou técnico responsável | Altera a prioridade |
| `POST` | `/api/tickets/{id}/close` | Conforme regras | Encerra o chamado |
| `POST` | `/api/tickets/{id}/reopen` | Conforme regras | Reabre o chamado |

### Comentários

| Método | Caminho | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/tickets/{ticketId}/comments` | Com acesso ao chamado | Adiciona comentário |
| `GET` | `/api/tickets/{ticketId}/comments` | Com acesso ao chamado | Lista comentários |

---

## Exemplos de uso

Os exemplos abaixo assumem que o token está na variável `TOKEN`.

### Criar um técnico (ADMIN)

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Técnico",
    "email": "joao.tecnico@example.com",
    "password": "senha-forte-123",
    "role": "TECHNICIAN"
  }'
```

### Abrir um chamado

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Notebook não conecta ao Wi-Fi",
    "description": "O notebook corporativo não encontra a rede Wi-Fi do escritório.",
    "category": "NETWORK",
    "priority": "HIGH"
  }'
```

Resposta `201 Created`:

```json
{
  "success": true,
  "message": "Chamado aberto com sucesso.",
  "data": {
    "id": 10,
    "title": "Notebook não conecta ao Wi-Fi",
    "description": "O notebook corporativo não encontra a rede Wi-Fi do escritório.",
    "category": "NETWORK",
    "priority": "HIGH",
    "status": "OPEN",
    "requester": {
      "id": 1,
      "name": "Maria Souza",
      "email": "maria@example.com",
      "role": "REQUESTER"
    },
    "createdAt": "2026-11-08T17:45:00Z",
    "updatedAt": "2026-11-08T17:45:00Z"
  },
  "timestamp": "2026-11-08T17:45:00Z"
}
```

> O campo `technician` é omitido enquanto o chamado não tiver responsável, pois a serialização ignora valores nulos.

### Listar chamados com filtros

```bash
curl "http://localhost:8080/api/tickets?status=OPEN&priority=HIGH&category=NETWORK&page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"
```

### Consultar um chamado

```bash
curl http://localhost:8080/api/tickets/10 \
  -H "Authorization: Bearer $TOKEN"
```

### Atualizar conteúdo do chamado

```bash
curl -X PUT http://localhost:8080/api/tickets/10 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Notebook não conecta ao Wi-Fi",
    "description": "Atualização: o problema ocorre apenas na sala 3.",
    "category": "NETWORK",
    "priority": "CRITICAL"
  }'
```

### Técnico assume o chamado

```bash
curl -X PATCH http://localhost:8080/api/tickets/10/assign \
  -H "Authorization: Bearer $TECH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "technicianId": 2
  }'
```

### Alterar status

```bash
curl -X PATCH http://localhost:8080/api/tickets/10/status \
  -H "Authorization: Bearer $TECH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

### Alterar prioridade

```bash
curl -X PATCH http://localhost:8080/api/tickets/10/priority \
  -H "Authorization: Bearer $TECH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "priority": "CRITICAL"
  }'
```

### Encerrar e reabrir

```bash
curl -X POST http://localhost:8080/api/tickets/10/close \
  -H "Authorization: Bearer $TECH_TOKEN"

curl -X POST http://localhost:8080/api/tickets/10/reopen \
  -H "Authorization: Bearer $TOKEN"
```

### Comentar

```bash
curl -X POST http://localhost:8080/api/tickets/10/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "O problema continua após reiniciar o notebook."
  }'
```

Resposta `201 Created`:

```json
{
  "success": true,
  "message": "Comentário adicionado com sucesso.",
  "data": {
    "id": 5,
    "ticketId": 10,
    "author": {
      "id": 1,
      "name": "Maria Souza",
      "email": "maria@example.com",
      "role": "REQUESTER"
    },
    "content": "O problema continua após reiniciar o notebook.",
    "createdAt": "2026-11-08T17:50:00Z"
  },
  "timestamp": "2026-11-08T17:50:00Z"
}
```

### Atualizar o próprio perfil

```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Souza Lima",
    "currentPassword": "senha-forte-123",
    "newPassword": "nova-senha-forte-456"
  }'
```

Para alterar apenas o nome, envie somente `name`. Se `newPassword` for informado, `currentPassword` passa a ser obrigatório.

---

## Paginação, ordenação e filtros

Endpoints de listagem aceitam os parâmetros padrão do Spring Data:

| Parâmetro | Exemplo | Descrição |
|---|---|---|
| `page` | `0` | Número da página, iniciando em zero |
| `size` | `20` | Tamanho da página; máximo 100 |
| `sort` | `createdAt,desc` | Campo e direção; pode repetir |

### Filtros de chamados

| Parâmetro | Valores |
|---|---|
| `status` | `OPEN`, `IN_PROGRESS`, `WAITING_FOR_USER`, `RESOLVED`, `CLOSED` |
| `priority` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `category` | `HARDWARE`, `SOFTWARE`, `NETWORK`, `ACCESS`, `OTHER` |

### Campos de ordenação permitidos

- **Chamados**: `id`, `title`, `status`, `priority`, `category`, `createdAt`, `updatedAt`
- **Comentários**: `id`, `createdAt`
- **Usuários**: `id`, `name`, `email`, `role`, `active`, `createdAt`, `updatedAt`

Ordenações padrão:

- Chamados: `createdAt desc`, `id desc`
- Comentários: `createdAt asc`, `id asc`
- Usuários: `createdAt desc`, `id desc`

Campo de ordenação inválido retorna `422 Unprocessable Entity`.

---

## Formato das respostas

### Sucesso

```json
{
  "success": true,
  "message": "Mensagem descritiva.",
  "data": {},
  "timestamp": "2026-11-08T17:42:00Z"
}
```

### Listagem paginada

O campo `data` segue o formato:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### Erro

```json
{
  "timestamp": "2026-11-08T17:42:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Transição de status inválida: OPEN -> RESOLVED",
  "path": "/api/tickets/10/status"
}
```

### Erro de validação

```json
{
  "timestamp": "2026-11-08T17:42:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validação nos dados enviados.",
  "path": "/api/tickets",
  "fieldErrors": {
    "description": "A descrição deve ter entre 10 e 4000 caracteres",
    "category": "A categoria é obrigatória"
  }
}
```

### Códigos HTTP usados

| Código | Situação |
|---|---|
| `200` | Consulta ou atualização bem-sucedida |
| `201` | Recurso criado |
| `400` | Dados inválidos ou malformados |
| `401` | Token ausente, inválido ou credenciais incorretas |
| `403` | Perfil sem permissão para a operação |
| `404` | Recurso não encontrado |
| `409` | E-mail duplicado ou violação de integridade |
| `422` | Regra de negócio violada, incluindo transição de status inválida |
| `500` | Erro inesperado |

---

## Testes

A suíte cobre testes unitários e testes de integração com `MockMvc`, usando H2 em modo PostgreSQL. Nenhum teste depende de um PostgreSQL externo.

Executar todos os testes:

```bash
mvn test
```

Executar apenas testes unitários:

```bash
mvn "-Dtest=*Test" test
```

Executar apenas testes de integração:

```bash
mvn "-Dtest=*IT" test
```

Gerar o pacote executável, executando os testes:

```bash
mvn package
```

> Situação verificada neste projeto: **127 testes**, sendo 74 unitários, 52 de integração dos controllers e 1 teste de migration Flyway, todos passando. O build gera `target/deskflow-1.0.0.jar`.

Cobertura funcional dos testes:

- cadastro, login, token inválido, autorização e permissões;
- criação, consulta, atualização, listagem e filtros de chamados;
- atribuição, prioridade, encerramento e reabertura;
- transições de status válidas e inválidas;
- comentários e bloqueio em chamados encerrados;
- administração de usuários;
- validação de DTOs e erros padronizados;
- migration Flyway e validação do mapeamento JPA.

---

## Estrutura do projeto

```text
.
├── scripts/
│   ├── setup-local-db.ps1   # Cria o cluster PostgreSQL local dedicado
│   ├── start-local-db.ps1   # Inicia o cluster local
│   ├── stop-local-db.ps1    # Para o cluster local
│   └── run-dev.ps1          # Inicia o banco e a API juntos
├── src/
│   ├── main/
│   │   ├── java/com/deskflow/
│   │   │   ├── config/          # OpenAPI, CORS, JWT e bootstrap do admin
│   │   │   ├── controller/      # Endpoints REST
│   │   │   ├── dto/             # Records de entrada, saída e respostas padronizadas
│   │   │   ├── entity/          # Entidades JPA e enums
│   │   │   ├── exception/       # Exceções e tratamento global de erros
│   │   │   ├── mapper/          # Conversão entidade -> DTO
│   │   │   ├── repository/      # Repositórios Spring Data JPA
│   │   │   ├── security/        # JWT, filtro, UserDetails e SecurityConfig
│   │   │   ├── service/         # Regras de negócio
│   │   │   │   └── support/     # Políticas de acesso, transição e paginação
│   │   │   └── DeskFlowApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/V1__create_tables.sql
│   └── test/
│       ├── java/com/deskflow/
│       │   ├── integration/     # Testes de integração com MockMvc
│       │   ├── security/        # Testes do JWT
│       │   ├── service/         # Testes unitários dos serviços e policies
│       │   └── support/         # Base compartilhada dos testes
│       └── resources/application-test.yml
├── .env                         # Configuração local, ignorada pelo Git
├── .env.example
├── .postgres/                   # Cluster local opcional, ignorado pelo Git
└── pom.xml
```

---

## Decisões técnicas

- **Regras de negócio nos serviços e policies**: controllers apenas orquestram a entrada e saída HTTP.
- **DTOs como records**: respostas explícitas, sem serializar entidades JPA.
- **JWT stateless**: sem sessão HTTP, com `SessionCreationPolicy.STATELESS`.
- **Usuário recarregado por requisição**: alterações de perfil ou desativação passam a valer imediatamente.
- **BCrypt**: hash de senha com fator padrão do Spring Security.
- **Flyway + `ddl-auto: validate`**: o schema é criado por migration e validado pelo Hibernate.
- **H2 em modo PostgreSQL nos testes**: integração rápida e sem infraestrutura externa.
- **Módulo `spring-boot-flyway`**: necessário no Spring Boot 4 para ativar a auto-configuração do Flyway.
- **Bootstrap de admin opcional**: facilita o primeiro acesso sem criar usuários manualmente no banco.
- **`.env` opcional**: conveniência local, sem exigir biblioteca externa.

### Limitações conhecidas

- Não há endpoint de desativação/exclusão de usuários nesta versão.
- Não há refresh token; ao expirar, é necessário autenticar novamente.
- Não há upload de anexos nos chamados.
- O projeto não inclui Docker; a execução usa um PostgreSQL local.
- Não há rate limiting nem bloqueio por tentativas de login.

---

## Licença

Projeto de estudo e portfólio. Sinta-se livre para utilizar como referência.
