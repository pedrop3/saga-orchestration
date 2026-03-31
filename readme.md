# 🧩 Saga Orchestration with AI Agents — LangChain4j + MCP

A distributed microservices architecture implementing the **Saga Pattern** with **AI-powered orchestration** using [LangChain4j](https://langchain4j.dev). Three intelligent agents — powered by Gemini, Ollama, and pgvector — automatically diagnose failures, compose dynamic saga plans, and answer operational questions in natural language.


---

## 📐 Architecture Overview

The system consists of **6 microservices** communicating via **Kafka** and an **AI agent service** that connects to all of them via **MCP (Model Context Protocol)**.

```
                         ┌─────────────────────┐
                         │   ai-saga-agent      │
                         │   port: 8099         │
                         │   Gemini · MCP · RAG │
                         └────────┬────────────┘
                                  │ MCP (HTTP/SSE)
            ┌─────────────┬───────┼────────┬──────────────┐
            ▼             ▼       ▼        ▼              ▼
    ┌──────────────┐ ┌──────────┐ ┌─────────────┐ ┌─────────────────┐
    │ order-service│ │orchestr. │ │payment-svc  │ │inventory-svc    │
    │ port: 3000   │ │port: 8050│ │port: 8091   │ │port: 8092       │
    │ MongoDB      │ │Redis     │ │PostgreSQL   │ │PostgreSQL       │
    └──────────────┘ └──────────┘ └─────────────┘ └─────────────────┘
                                  ┌─────────────────┐
                                  │product-valid-svc │
                                  │port: 8090        │
                                  │PostgreSQL        │
                                  └─────────────────┘
```

### Saga Flow (Happy Path)

```
Order Service → Orchestrator → Product Validation ✅ → Payment ✅ → Inventory ✅ → Finish Success
```

If any step fails, compensating transactions roll back the previous steps automatically.

### The 3 AI Agents

| Agent | Trigger | What it does | Storage |
|-------|---------|-------------|---------|
| **OperationsAgent** | Kafka `notify-ending` (FAIL) | Auto-diagnoses saga failures using RAG over historical incidents | pgvector + PostgreSQL |
| **SagaComposerAgent** | Scheduled (every 60s dev / 30min prod) | Decides optimal step order per customer profile | Redis `saga-plan:{profile}` |
| **DataAnalystAgent** | HTTP `GET /api/agent/chat?question=...` | Answers operational questions in natural language via MCP tools | Human-readable response |

---

## ⚙️ Prerequisites

Before running the project, make sure you have the following installed:

| Tool | Version | Purpose |
|------|---------|---------|
| **Java JDK** | 21+ | All microservices |
| **Docker & Docker Compose** | Latest | Databases, Kafka, Redis |
| **Ollama** | Latest | Local embedding model (nomic-embed-text) |
| **Gemini API Key** | — | Primary LLM for agents (free tier available) |

### Optional

| Tool | Version | Purpose |
|------|---------|---------|
| **Claude API Key** | — | Alternative LLM provider |
| **Gradle** | 8.11+ | Included via wrapper (`./gradlew`) |

---

## 🔑 API Keys Setup

The AI agent service requires at least one LLM API key. The default configuration uses **Gemini**.

### 1. Get a Gemini API Key (Required)

1. Go to [Google AI Studio](https://aistudio.google.com/apikey)
2. Create a new API key
3. Export it as an environment variable:

```bash
export GEMINI_API_KEY=your-gemini-api-key-here
```

### 2. Claude API Key (Optional)

If you want to use Claude as the LLM provider:

1. Get an API key from [Anthropic Console](https://console.anthropic.com/)
2. Export it:

```bash
export CLAUDE_API_KEY=your-claude-api-key-here
```

Then change the primary model in `ai-saga-agent/src/main/resources/application.yml`:

```yaml
ai:
  primary-model: claude  # change from 'gemini' to 'claude'
```

### 3. Install Ollama and Pull the Embedding Model (Required)

Ollama runs locally and is used for generating embeddings (RAG). It's free and no API key is needed.

```bash
# Install Ollama
brew install ollama          # macOS
# or: curl -fsSL https://ollama.ai/install.sh | sh   # Linux

# Pull the embedding model
ollama pull nomic-embed-text

# Start the Ollama server
ollama serve
# API runs at http://localhost:11434
```

---

## 🚀 How to Run

There are two ways to run the project. In both cases, the **ai-saga-agent** runs outside Docker (it needs access to Ollama and your API keys).

### Option A — Docker Compose (recommended)

Runs the 5 core microservices + all infrastructure in Docker. Only the AI agent runs locally.

> ⚠️ **You must build the JARs before running `docker-compose`**, because each `Dockerfile` copies the pre-compiled JAR (`COPY build/libs/*.jar app.jar`). Without the build, the containers will fail.

```bash
# 1. Build all JARs (publishes saga-commons + compiles all services)
chmod +x build-all.sh
./build-all.sh

# 2. Start everything (infra + 5 microservices)
docker-compose up --build -d

# 3. Wait for all containers to be healthy, then start the AI agent separately
cd ai-saga-agent && GEMINI_API_KEY=your-key-here ./gradlew bootRun
```

This starts:

| Container | Port | Type |
|-----------|------|------|
| `order-db` (MongoDB) | 27017 | Infrastructure |
| `product-db` (PostgreSQL) | 5432 | Infrastructure |
| `payment-db` (PostgreSQL) | 5433 | Infrastructure |
| `inventory-db` (PostgreSQL) | 5434 | Infrastructure |
| `vectors-db` (pgvector) | 5435 | Infrastructure |
| `redis` | 6379 | Infrastructure |
| `kafka` | 9092 | Infrastructure |
| `redpanda` (Kafka UI) | 8081 | Infrastructure |
| `prometheus` | 9090 | Monitoring |
| `grafana` | 3001 | Monitoring |
| `order-service` | 3000 | Microservice |
| `orchestrator-service` | 8050 | Microservice |
| `product-validation-service` | 8090 | Microservice |
| `payment-service` | 8091 | Microservice |
| `inventory-service` | 8092 | Microservice |
| **ai-saga-agent** (local) | 8099 | AI Agent (manual) |

### Option B — All Local (no Docker for services)

Runs only infrastructure in Docker; all microservices run via Gradle. Useful for development and debugging.

```bash
# 1. Start only infrastructure (databases, Kafka, Redis)
docker-compose up -d order-db product-db payment-db inventory-db vectors-db redis kafka redpanda-console

# 2. Build everything
chmod +x build-all.sh
./build-all.sh

# 3. Start each service in a separate terminal
cd orchestrator-service && ./gradlew bootRun
cd product-validation-service && ./gradlew bootRun
cd payment-service && ./gradlew bootRun
cd inventory-service && ./gradlew bootRun
cd order-service && ./gradlew bootRun

# 4. Start the AI agent (requires GEMINI_API_KEY + Ollama running)
cd ai-saga-agent && GEMINI_API_KEY=your-key-here ./gradlew bootRun
```

### Verify Everything is Running

```bash
curl http://localhost:3000/actuator/health   # order-service
curl http://localhost:8050/actuator/health   # orchestrator
curl http://localhost:8090/actuator/health   # product-validation
curl http://localhost:8091/actuator/health   # payment-service
curl http://localhost:8092/actuator/health   # inventory-service
curl http://localhost:8099/actuator/health   # ai-saga-agent
```

### Build Script Options

```bash
./build-all.sh --parallel           # Build all in parallel (faster)
./build-all.sh --with-tests         # Include unit tests
./build-all.sh ai-saga-agent        # Build only a specific service
./build-all.sh --help               # Show all options
```

---

## 🧪 Testing the System

> 📦 **Ready-to-use request collection included!** The repo contains a `Saga-Bruno.zip` with all API requests pre-configured. Import it into [Bruno](https://www.usebruno.com/), Insomnia, or any OpenAPI-compatible client.

### 1. Create an Order (Triggers a Saga)

```bash
curl -X POST http://localhost:3000/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "products": [
      {
        "product": { "code": "COMIC_BOOKS", "unitValue": 15.50 },
        "quantity": 3
      },
      {
        "product": { "code": "BOOKS", "unitValue": 9.90 },
        "quantity": 1
      }
    ],
    "customerId": "customer-001",
    "clientType": "new"
  }'
```

Valid product codes: `COMIC_BOOKS`, `BOOKS`, `MOVIES`, `MUSIC`

### 2. Monitor Kafka Topics

Open the Redpanda Console at [http://localhost:8081](http://localhost:8081) to see events flowing through topics.

### 3. Ask the AI Agent a Question

```bash
# Natural language query via the DataAnalystAgent
curl "http://localhost:8099/api/agent/chat?question=List%20the%205%20most%20recent%20failed%20sagas%20and%20assess%20their%20fraud%20risk"
```

### 4. View AI Diagnostics

```bash
# See all auto-generated failure diagnostics
curl http://localhost:8099/api/agent/diagnostics
```

### 5. View Current Saga Plans (from SagaComposerAgent)

```bash
# See the AI-generated execution plans per customer profile
curl http://localhost:8099/api/agent/composer/plans
```

### 6. Filter Events by Order or Transaction

```bash
curl -X GET http://localhost:3000/api/events/filters \
  -H "Content-Type: application/json" \
  -d '{ "orderId": "YOUR_ORDER_ID", "transactionId": "" }'
```

### 7. Test MCP Tools Directly

You can test the MCP protocol manually against any service. First open an SSE session, then send JSON-RPC messages:

```bash
# 1. Open SSE session (returns a sessionId)
curl http://localhost:3000/sse

# 2. Initialize the MCP connection
curl -X POST "http://localhost:3000/mcp/message?sessionId=YOUR_SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0", "id": 1, "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "clientInfo": { "name": "test-client", "version": "1.0.0" },
      "capabilities": {}
    }
  }'

# 3. List available tools
curl -X POST "http://localhost:3000/mcp/message?sessionId=YOUR_SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{ "jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {} }'

# 4. Execute a tool
curl -X POST "http://localhost:3000/mcp/message?sessionId=YOUR_SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0", "id": 3, "method": "tools/call",
    "params": {
      "name": "getStockByProduct",
      "arguments": { "productCode": "COMIC_BOOKS" }
    }
  }'
```

---

## 🌐 API Reference

### Order Service (`http://localhost:3000`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/orders` | Create a new order (triggers the saga) |
| `GET` | `/api/events` | List all saga events |
| `GET` | `/api/events/filters` | Filter events by `orderId` or `transactionId` |

Swagger UI available at: [http://localhost:3000/swagger-ui/index.html](http://localhost:3000/swagger-ui/index.html)

### AI Agent Service (`http://localhost:8099`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/agent/chat?question=...` | Ask the DataAnalystAgent a natural language question |
| `GET` | `/api/agent/diagnostics` | List all auto-generated failure diagnostics |
| `GET` | `/api/agent/composer/plans` | View current AI-generated saga plans per profile |

### MCP Endpoints (all services)

| Service | SSE Endpoint | Message Endpoint |
|---------|-------------|-----------------|
| order-service | `GET http://localhost:3000/sse` | `POST http://localhost:3000/mcp/message?sessionId=...` |
| product-validation | `GET http://localhost:8090/sse` | `POST http://localhost:8090/mcp/message?sessionId=...` |
| payment-service | `GET http://localhost:8091/sse` | `POST http://localhost:8091/mcp/message?sessionId=...` |
| inventory-service | `GET http://localhost:8092/sse` | `POST http://localhost:8092/mcp/message?sessionId=...` |

---

## 🗂️ Project Structure

```
saga-orchestration/
├── saga-commons/                  # Shared DTOs, enums, utilities (published to mavenLocal)
├── order-service/                 # REST API + MongoDB + Kafka producer
├── orchestrator-service/          # Saga state machine + Redis plan lookup
├── product-validation-service/    # Product catalog validation + MCP server
├── payment-service/               # Payment + fraud scoring + MCP server
├── inventory-service/             # Stock management + MCP server
├── ai-saga-agent/                 # 3 AI agents (Gemini + Ollama + pgvector + MCP client)
├── docker-compose.yml             # Full infrastructure stack
├── build-all.sh                   # One-command build script
├── bruno-collection.zip           # Pre-configured API request collection
└── readme.md
```

---

## 🔧 Configuration Reference

### Environment Variables

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `GEMINI_API_KEY` | — | **Yes** (if using Gemini) | Google AI Gemini API key |
| `CLAUDE_API_KEY` | — | No | Anthropic Claude API key |
| `GEMINI_MODEL` | `gemini-2.5-flash` | No | Gemini model name |
| `CLAUDE_MODEL` | `claude-sonnet-4-20250514` | No | Claude model name |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | No | Ollama server URL |
| `OLLAMA_MODEL` | `qwen3:8b` | No | Ollama chat model (for local inference) |
| `KAFKA_BROKER` | `localhost:9092` | No | Kafka bootstrap servers |
| `REDIS_HOST` | `localhost` | No | Redis host |
| `VECTORS_DB_HOST` | `localhost` | No | pgvector PostgreSQL host |
| `VECTORS_DB_PORT` | `5435` | No | pgvector PostgreSQL port |
| `MONGO_DB_URI` | `mongodb://admin:123456@localhost:27017` | No | MongoDB connection URI |

### Switching the Primary AI Model

Edit `ai-saga-agent/src/main/resources/application.yml`:

```yaml
ai:
  primary-model: gemini    # options: gemini, claude, ollama, ollama-no-think
```

---

## 🧠 How the AI Layer Works

### MCP (Model Context Protocol)

Each microservice exposes an **MCP server** over HTTP/SSE, making its business logic available as tools that any AI agent can discover and invoke.

| Service | MCP Endpoint | Available Tools |
|---------|-------------|-----------------|
| order-service | `localhost:3000/sse` | `getOrderById`, `listRecentEvents`, `getLastEventByOrder` |
| payment-service | `localhost:8091/sse` | `getPaymentStatus`, `getRefundRate`, `getFraudRiskScore` |
| inventory-service | `localhost:8092/sse` | `getStockByProduct`, `getLowStockAlert`, `checkReservationExists` |
| product-validation | `localhost:8090/sse` | `checkProductExists`, `checkValidationExists`, `listCatalog` |

### RAG (Retrieval Augmented Generation)

The OperationsAgent vectorizes every saga event into **pgvector** using Ollama's `nomic-embed-text` model. When a saga fails, the agent searches for similar past incidents to enrich its diagnosis.

### Dynamic Saga Planning

The SagaComposerAgent periodically analyzes system metrics and historical patterns, then writes optimized saga step sequences to **Redis**. The orchestrator reads these plans to decide the execution order per customer profile (e.g., running fraud validation before payment for new high-value customers).

---

## 📦 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 / 4.0 |
| AI SDK | LangChain4j 1.11 |
| LLM (cloud) | Google Gemini 2.5 Flash |
| LLM (local) | Ollama (qwen3:8b / nomic-embed-text) |
| Vector DB | PostgreSQL + pgvector |
| Messaging | Apache Kafka |
| Cache | Redis 7 |
| Databases | MongoDB, PostgreSQL |
| Build | Gradle |
| Containers | Docker Compose |
| Monitoring | Prometheus + Grafana |

---

## 🐛 Troubleshooting

### "Connection refused" on MCP endpoints

Make sure all microservices are running before starting the `ai-saga-agent`. The agent tries to connect to MCP servers on startup.

### AI agent returns empty or error responses

- Verify `GEMINI_API_KEY` is set and valid
- Check that Ollama is running (`ollama serve`) and the `nomic-embed-text` model is pulled
- Increase `maxOutputTokens` in `ChatModelConfig.java` if responses are truncated (default is 1024; use 4096 for complex queries)

### `saga-commons` build failure

All services depend on `saga-commons`. Run the build script or publish it manually:

```bash
cd saga-commons && ./gradlew publishToMavenLocal
```

### Kafka topics not created

Ensure Kafka is fully started before launching services. Check Redpanda Console at `http://localhost:8081`.

---

## 📚 Key Lessons from the Project

1. **MCP > @Tool for microservices** — reuse business logic across any agent without coupling
2. **SystemMessage alignment is critical** — tools described in the prompt that don't exist cause silent failures
3. **JSON responses win over key=value** — `ObjectMapper.writeValueAsString()` is one line, zero bugs
4. **Workflow instructions > tool descriptions** — tell the agent HOW to use tools, not just WHAT they do
5. **maxOutputTokens matters** — 1024 isn't enough for 5 sagas + fraud scores; use 4096
6. **Virtual threads are essential** — `spring.threads.virtual.enabled=true` enables parallel MCP calls at no cost

---

## 💼 Roadmap & Challenges

Here are some enhancements and learning challenges to evolve the project:

### Architecture & Design

- [ ] Implement Hexagonal Architecture (Ports & Adapters)
- [ ] Extract Kafka message contracts into a shared module
- [ ] Create integration tests using Embedded Kafka
- [ ] Add support for the Outbox pattern
- [ ] Implement Liquibase

### Observability & Monitoring

- [ ] Add Micrometer + Prometheus metrics
- [x] Implement structured logging with correlation IDs
- [ ] Publish Grafana dashboards
- [ ] Implement ELK for logs

### Core Features

- [ ] Add compensation logic in payment-service
- [ ] Support multi-step saga with dynamic ordering (e.g., payment → shipment → invoice)
- [ ] Add saga status endpoint

### Security

- [ ] Add JWT-based authentication
- [ ] Implement Oauth Server
- [ ] Restrict Kafka topic access with ACL or SASL

### DevOps & Infrastructure

- [x] Create Docker Compose environment (Kafka + PostgreSQL + Services)
- [ ] Set up CI/CD with GitHub Actions
- [ ] Add Kubernetes readiness and liveness probes
- [ ] Implement test with Jmeter

### Documentation

- [ ] Generate OpenAPI (Swagger) docs
- [ ] Include event orchestration sequence diagram
- [ ] Add guide for local mock testing

### Testing & Quality

- [ ] Achieve 80%+ test coverage
- [ ] Implement to report to test coverage
- [ ] Add end-to-end tests with Testcontainers

### Advanced Architecture

- [ ] Introduce Domain Events and Event Sourcing
- [ ] Implement Saga Timeout Handling
- [ ] Support parallel saga steps
- [ ] Load saga flow from JSON/YAML config
- [ ] Use state machine library to manage saga steps

### Resilience & Fault Tolerance

- [ ] Add retry and backoff policies for Kafka consumers
- [ ] Use circuit breakers
- [ ] Persist saga history in a dedicated table
- [ ] Configure Kafka Dead Letter Topics (DLT)

### Developer Experience

- [x] Provide Postman or Insomnia collection
- [ ] Create mock implementations for dependencies
- [ ] Add Makefile or CLI utility scripts
- [ ] Build a saga dashboard UI (React/Vue)

### Scalability & Performance

- [ ] Make Kafka topics configurable per environment
- [ ] Add event sharding by saga ID
- [ ] Configure microservices for horizontal scaling

### Internationalization & Multi-Tenancy

- [ ] Add multi-tenant support via headers or topics
- [ ] Support localization of logs and messages
- [ ] Add tenant-aware metrics and logs

### Kafka + Schema Registry Enhancements

**Avro/Protobuf Integration**
- [ ] Migrate from JSON to Avro or Protobuf
- [ ] Maintain a central schema repository

**Schema Registry Setup**
- [ ] Integrate Confluent Schema Registry (via Docker)
- [ ] Set appropriate subject naming strategy
- [ ] Secure access with basic auth or API keys

**Validation & Compatibility**
- [ ] Enforce schema compatibility rules (backward/forward)
- [ ] Add CI step for schema validation
- [ ] Test schema evolution scenarios

**Development & Testing**
- [ ] Use MockSchemaRegistry in tests
- [ ] Generate Avro classes from .avsc files

**Monitoring**
- [ ] Monitor schema usage with Confluent Control Center
- [ ] Log schema version and validation errors

### Functional Enhancements

**✅ Saga Manager Dashboard**
- [ ] Build a real-time dashboard for saga tracking
- [ ] Allow manual retry/restart of sagas

**🧾 Audit & History**
- [x] Store full saga execution history
- [x] Create endpoint to fetch history by saga ID

**📤 Webhooks & Notifications**
- [ ] Allow webhook subscriptions for saga completion
- [ ] Integrate with Slack or email for alerts

**🧭 Dynamic Orchestration**
- [ ] Support saga definitions via JSON/YAML
- [ ] Design a DSL for saga steps and compensations

**♻️ Manual Retry & Reprocessing**
- [ ] Add endpoint to reprocess events by saga ID
- [ ] Support execution of compensation steps only

**🌍 Multi-Region & Partition Tolerance**
- [ ] Support distributed saga execution across regions
- [ ] Use Kafka MirrorMaker 2.0 for topic replication

---

## 🔗 Resources

- **LangChain4j docs**: [langchain4j.dev](https://langchain4j.dev)

---

## ✅ Author
**Pedro Santos** — [LinkedIn](https://linkedin.com/in/pedrohmsantos)