# Kanban API - Spring Boot Backend

A RESTful API for managing Kanban board tasks, built with Spring Boot 3.x.

## 📋 Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3.4.4 |
| Language | Java 17 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Security | Spring Security + JWT |
| Rate Limiting | Bucket4j + Caffeine |
| Caching | Caffeine Cache |
| Real-time | WebSocket + STOMP + SockJS |
| Documentation | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven |
| Containerization | Docker, Docker Compose |

## 🚀 Quick Start

### Prerequisites

- Java 17
- Docker & Docker Compose
- Maven 3.9+

### Option 1: Run with Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-username/kanban-api.git
cd kanban-api

# Start the application with PostgreSQL
docker-compose up -d

# The API will be available at http://localhost:8080
```

### Option 2: Run Locally

```bash
# Start PostgreSQL (using Docker)
docker run -d \
  --name kanban-postgres \
  -e POSTGRES_DB=kanban \
  -e POSTGRES_USER=kanban \
  -e POSTGRES_PASSWORD=kanban \
  -p 5432:5432 \
  postgres:16-alpine

# Build and run the application
mvn spring-boot:run
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run with Coverage Report

```bash
mvn verify

# Coverage report available at: target/site/jacoco/index.html
```

### Run Integration Tests Only

```bash
mvn test -Dtest="*IntegrationTest"
```

> **Note**: Integration tests require Docker for Testcontainers.


## 📊 Observability

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Prometheus Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

## 🐳 Docker

### Build Image

```bash
docker build -t kanban-api .
```

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | Database host |
| DB_PORT | 5432 | Database port |
| DB_NAME | kanban | Database name |
| DB_USERNAME | kanban | Database username |
| DB_PASSWORD | kanban | Database password |
| JWT_SECRET | - | JWT signing secret (min 256 bits) |
| JWT_EXPIRATION | 86400000 | JWT expiration in ms (24h) |
| RATE_LIMIT_REQUESTS_PER_MINUTE | 100 | Rate limit requests per minute per IP |
| SERVER_PORT | 8080 | Application port |

## 📝 Task Model

### Status

- `TO_DO` - Task not started
- `IN_PROGRESS` - Task in progress
- `DONE` - Task completed

### Priority

- `LOW` - Low priority
- `MEDIUM` - Medium priority
- `HIGH` - High priority