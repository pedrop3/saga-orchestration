# 🧩 Saga Orchestration Microservices Architecture – README

## 🧾 Project Overview

This project implements a distributed microservices architecture based on the **Saga Pattern**, designed to handle complex transactions across multiple services. The architecture uses event-driven communication and central orchestration to ensure consistency, traceability, and reliability across all operations.

---

## 🔀 Services Overview

### 🛒 **Order-Service**
- Exposes REST endpoints to initiate orders.
- Stores initial order data and the events received.
- Database: **MongoDB**.

### 🧠 **Orchestrator-Service**
- Central brain of the system.
- Responsible for directing the saga flow: it tracks which services have been executed and manages transitions to the next.
- Saves the orchestration process state.
- Stateless (no database).

### 🧾 **Product-Validation-Service**
- Validates whether ordered products exist and are valid.
- Stores validation results per order ID.
- Database: **PostgreSQL**.

### 💳 **Payment-Service**
- Calculates and processes the payment based on order quantity and unit price.
- Stores payment status.
- Database: **PostgreSQL**.

### 📦 **Inventory-Service**
- Updates inventory by subtracting ordered quantities.
- Stores product quantity debits for each order.
- Database: **PostgreSQL**.

---

## 🧠 Saga Orchestration Flow

The saga follows a strict flow defined and controlled by the **Orchestrator-Service**. Each step emits and listens to events via message brokers
### 🖼️ Architectural Drawing
![Architectural Drawing](./architectural%20drawing.png)

### 🖼️ Saga Orchestration Diagram
![Saga Orchestration](./SagaOrchestrator.png)

### 🖼️ Service Topics / Communication
![Service Topics](ServiceTopic.png)

---

## 🐳 Running the Project with Docker Compose

Make sure you have Docker and Docker Compose installed.

### ▶️ Start the System
```bash
docker-compose up --build
```

This will spin up all services, including MongoDB and PostgreSQL databases.

---

## 🌐 API Access & Documentation

Each microservice exposes its own API

- Order Service: [http://localhost:3000/swagger-ui.html](http://localhost:3000/swagger-ui/index.html)
- RedPanda: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)

---

## 📦 Technologies Used

- Java 21
- Spring Boot
- MongoDB & PostgreSQL
- Docker & Docker Compose
- Event-driven architecture (Kafka)
- OpenAPI (Swagger)

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
- [X] Implement structured logging with correlation IDs
- [ ] Publish Grafana dashboards
- [ ] Implement ELK for logs

Core Features

- [ ] Add compensation logic in payment-service
- [ ] Support multi-step saga with dynamic ordering (e.g., payment → shipment → invoice)
- [ ] Add saga status endpoint

### Security

- [ ] Add JWT-based authentication
- [ ] Implement Oauth Server
- [ ] Restrict Kafka topic access with ACL or SASL

### DevOps & Infrastructure

- [X] Create Docker Compose environment (Kafka + PostgreSQL + Services)
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

- [ ] Provide Postman or Insomnia collection
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

## Kafka + Schema Registry Enhancements

### Avro/Protobuf Integration

- [ ] Migrate from JSON to Avro or Protobuf
- [ ] Maintain a central schema repository

### Schema Registry Setup

- [ ] Integrate Confluent Schema Registry (via Docker)
- [ ] Set appropriate subject naming strategy
- [ ] Secure access with basic auth or API keys

### Validation & Compatibility

- [ ] Enforce schema compatibility rules (backward/forward)
- [ ] Add CI step for schema validation
- [ ] Test schema evolution scenarios

### Development & Testing

- [ ] Use MockSchemaRegistry in tests
- [ ] Generate Avro classes from .avsc files

### Monitoring

- [ ] Monitor schema usage with Confluent Control Center
- [ ] Log schema version and validation errors

# Functional Enhancements

### ✅ Saga Manager Dashboard

- [ ] Build a real-time dashboard for saga tracking
- [ ] Allow manual retry/restart of sagas

### 🧾 Audit & History

- [x] Store full saga execution history
- [x] Create endpoint to fetch history by saga ID

### 📤 Webhooks & Notifications

- [ ] Allow webhook subscriptions for saga completion
- [ ] Integrate with Slack or email for alerts

### 🧭 Dynamic Orchestration

- [ ] Support saga definitions via JSON/YAML
- [ ] Design a DSL for saga steps and compensations

### ♻️ Manual Retry & Reprocessing

- [ ] Add endpoint to reprocess events by saga ID
- [ ] Support execution of compensation steps only

### 🌍 Multi-Region & Partition Tolerance

- [ ] Support distributed saga execution across regions
- [ ] Use Kafka MirrorMaker 2.0 for topic replication
---

## ✅ Author
Pedro Santos

> This architecture provides a robust and scalable foundation for distributed transaction management using the Saga pattern.

