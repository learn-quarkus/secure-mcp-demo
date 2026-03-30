# Secure MCP Demo Architecture

## System Architecture Diagram

```mermaid
graph TB
    subgraph "User Interaction"
        Browser[🌐 Browser/curl<br/>GET /poem]
    end

    subgraph "Client Layer - Port 8085"
        Client[📱 secure-mcp-client<br/>Quarkus + LangChain4j]
        OpenAI[🤖 OpenAI GPT-4<br/>Poem Generation]
    end

    subgraph "MCP Server Layer - Port 8080"
        MCP[🔧 secure-mcp-server<br/>MCP Tool Provider<br/>Token Exchange]
    end

    subgraph "REST API Layer - Port 8087"
        REST[🔒 secure-mcp-rest<br/>Service Account API]
    end

    subgraph "Identity Provider - Port 9080"
        Keycloak[🔐 Keycloak<br/>OIDC Provider<br/>Realm: quarkus]
    end

    Browser -->|HTTP Request| Client
    Client -->|1. Get Token<br/>aud: quarkus-mcp-server| Keycloak
    Client -->|2. Call MCP with Token| MCP
    MCP -->|3. Exchange Token<br/>aud: quarkus-mcp-service| Keycloak
    MCP -->|4. Call REST with Exchanged Token| REST
    REST -->|5. Return Service Account Name| MCP
    MCP -->|6. Return Tool Result| Client
    Client -->|7. Generate Poem| OpenAI
    Client -->|8. Return Poem| Browser

    style Browser fill:#e1f5ff
    style Client fill:#fff4e1
    style MCP fill:#ffe1f5
    style REST fill:#e1ffe1
    style Keycloak fill:#ffe1e1
    style OpenAI fill:#f0e1ff
```

## Authentication & Token Flow

```mermaid
sequenceDiagram
    autonumber
    participant User as 👤 User
    participant Client as 📱 Client<br/>(Port 8085)
    participant KC as 🔐 Keycloak<br/>(Port 9080)
    participant MCP as 🔧 MCP Server<br/>(Port 8080)
    participant REST as 🔒 REST API<br/>(Port 8087)
    participant AI as 🤖 OpenAI

    User->>Client: GET /poem

    Note over Client,KC: Client Authentication
    Client->>KC: Token Request (client_credentials)<br/>client_id: quarkus-mcp-client<br/>scope: quarkus-mcp-server-scope
    KC->>Client: Access Token<br/>aud: quarkus-mcp-server

    Note over Client,AI: LangChain4j Processing
    Client->>AI: User Message: Write poem about Java
    AI->>Client: Tool Call Required: get service account name

    Note over Client,MCP: MCP Tool Invocation
    Client->>MCP: POST /mcp<br/>Header: Authorization Bearer TOKEN
    MCP->>MCP: Validate Token<br/>audience: quarkus-mcp-server

    Note over MCP,KC: Token Exchange
    MCP->>KC: Token Exchange Request<br/>subject_token: original token<br/>scope: quarkus-mcp-service-scope
    KC->>MCP: Exchanged Access Token<br/>aud: quarkus-mcp-service

    Note over MCP,REST: REST API Call
    MCP->>REST: GET /service-account-name<br/>Header: Authorization Bearer EXCHANGED_TOKEN
    REST->>REST: Validate Token<br/>audience: quarkus-mcp-service
    REST->>MCP: service-account-quarkus-mcp-client

    MCP->>Client: Tool Result: service-account-quarkus-mcp-client

    Note over Client,AI: Final Poem Generation
    Client->>AI: Generate poem with service account name
    AI->>Client: Poem dedicated to service account

    Client->>User: 200 OK<br/>One-line poem about Java
```

## Component Details

### 🌐 Browser/User
- **Purpose**: Initiates the poem generation request
- **Endpoint**: `http://localhost:8085/poem`
- **Method**: GET

### 📱 Secure MCP Client (Port 8085)
- **Technology**: Quarkus + LangChain4j + OpenAI
- **OIDC Client**: `quarkus-mcp-client`
- **Capabilities**:
  - Acquires tokens using OAuth2 `client_credentials` grant
  - Communicates with MCP server
  - Orchestrates AI poem generation with OpenAI

### 🔧 Secure MCP Server (Port 8080)
- **Technology**: Quarkus + MCP Server Extension
- **OIDC Client**: `quarkus-mcp-server`
- **Capabilities**:
  - Provides MCP tool: `service-account-name-provider`
  - Validates incoming tokens (audience: `quarkus-mcp-server`)
  - Performs OAuth2 token exchange
  - Calls protected REST API with exchanged token

### 🔒 Secure MCP REST (Port 8087)
- **Technology**: Quarkus REST + OIDC
- **OIDC Tenant**: `service-account-name-rest-server`
- **Capabilities**:
  - Protected REST endpoint: `/service-account-name`
  - Validates tokens with audience: `quarkus-mcp-service`
  - Returns authenticated service account principal name

### 🔐 Keycloak (Port 9080)
- **Realm**: `quarkus`
- **Clients**:
  - `quarkus-mcp-client` (service account enabled)
  - `quarkus-mcp-server` (service account + token exchange enabled)
  - `quarkus-mcp-service` (audience target)
- **Client Scopes**:
  - `quarkus-mcp-server-scope` → adds `quarkus-mcp-server` audience
  - `quarkus-mcp-service-scope` → adds `quarkus-mcp-service` audience

### 🤖 OpenAI
- **Model**: GPT-4o-mini
- **Purpose**: Generates creative poem using service account information
- **Authentication**: API Key via `OPENAI_API_KEY` environment variable

## Security Model

### Token Audience Enforcement

```mermaid
graph LR
    subgraph "Token 1: Client → MCP Server"
        T1[Token<br/>aud: quarkus-mcp-server<br/>iss: service-account-quarkus-mcp-client]
    end

    subgraph "Token Exchange"
        EX[Keycloak<br/>Token Exchange<br/>Endpoint]
    end

    subgraph "Token 2: MCP Server → REST API"
        T2[Token<br/>aud: quarkus-mcp-service<br/>iss: service-account-quarkus-mcp-server]
    end

    T1 -->|Exchange| EX
    EX -->|New Token| T2

    style T1 fill:#ffe1e1
    style EX fill:#fff4e1
    style T2 fill:#e1ffe1
```

### Why Token Exchange?

1. **Audience Isolation**: Each service validates only tokens intended for it
2. **Principle of Least Privilege**: Tokens are scoped to specific audiences
3. **Security**: Prevents token reuse across different services
4. **Zero Trust**: Each service independently validates tokens

## Data Flow Example

### Request Flow
```
User Request: GET /poem
    ↓
Client acquires token (aud: quarkus-mcp-server)
    ↓
Client asks OpenAI to write a poem
    ↓
OpenAI determines it needs service account name
    ↓
Client calls MCP tool with token
    ↓
MCP validates token (aud: quarkus-mcp-server) ✓
    ↓
MCP exchanges token (aud: quarkus-mcp-service)
    ↓
MCP calls REST API with exchanged token
    ↓
REST validates token (aud: quarkus-mcp-service) ✓
    ↓
REST returns: "service-account-quarkus-mcp-client"
    ↓
MCP returns tool result to Client
    ↓
OpenAI generates poem with service account name
    ↓
Client returns poem to User
```

### Response Example
```
To service-account-quarkus-mcp-client,
Java brews strong logic like morning coffee,
powering dreams into compiled reality.
```

## Port Summary

| Service | Port | Purpose |
|---------|------|---------|
| Keycloak | 9080 | OIDC Provider & Token Exchange |
| MCP Server | 8080 | MCP Tool Provider |
| MCP Client | 8085 | LangChain4j Client |
| REST API | 8087 | Service Account Information |

## Environment Variables

| Variable | Used By | Purpose |
|----------|---------|---------|
| `QUARKUS_MCP_CLIENT_SECRET` | secure-mcp-client | Authenticate client to Keycloak |
| `QUARKUS_MCP_SERVER_SECRET` | secure-mcp-server | Authenticate server for token exchange |
| `OPENAI_API_KEY` | secure-mcp-client | Authenticate with OpenAI API |

## Key Technologies

- **Quarkus**: Java framework for cloud-native applications
- **LangChain4j**: Java library for building LLM applications
- **MCP (Model Context Protocol)**: Protocol for AI tool integration
- **OIDC (OpenID Connect)**: Authentication protocol
- **OAuth2 Token Exchange**: RFC 8693 token exchange mechanism
- **Keycloak**: Open-source identity and access management
- **OpenAI**: AI language model provider

## Security Features

- ✅ **Zero hardcoded credentials** - All secrets via environment variables
- ✅ **Token audience validation** - Each service validates correct audience
- ✅ **Token exchange** - Prevents token reuse across services
- ✅ **Service account authentication** - No user interaction required
- ✅ **OIDC standard compliance** - Industry-standard authentication
- ✅ **Mutual TLS ready** - Can be enhanced with mTLS
- ✅ **Proper logging** - JBoss logging instead of System.out
