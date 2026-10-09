# AMOS Aircraft Maintenance System

Enterprise maintenance operations platform: work orders, RBAC, workflow, Kafka notifications, and audit.

## Stack

- Backend: Spring Boot 3.2 (Java 17), Spring Security (JWT), JPA, Flyway
- Frontend: React 18, TypeScript, Vite, Tailwind CSS
- Database: PostgreSQL 16
- Events: Apache Kafka (KRaft)

## Quick start

### 1. Infrastructure

```bash
docker compose up -d postgres kafka
```

Wait until Postgres is healthy (`docker compose ps`).

Full stack (API + UI) after images build:

```bash
docker compose --profile app up --build
```

UI: `http://localhost:8088` · API: `http://localhost:8080`

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

If the Maven wrapper is not present:

```bash
cd backend
mvn spring-boot:run
```

API: `http://localhost:8080`  
Health: `GET /api/health`

On first boot, demo passwords are aligned to `Password123!`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` (proxies `/api` to the backend).

### Demo users

Password for all accounts: `Password123!`

| Username | Role |
|---|---|
| admin | Administrator |
| qa | Quality Assurance |
| manager | Maintenance Manager |
| supervisor | Maintenance Supervisor |
| engineer | Flight Engineer |
| tech1 / tech2 | Maintenance Technician |

## Tests

```bash
cd backend
mvn test
```

## Configuration

| Variable | Default |
|---|---|
| `DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/amos` |
| `DATASOURCE_USERNAME` / `DATASOURCE_PASSWORD` | `amos` / `amos` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `JWT_SECRET` | change in production |
| `GROK_PROVIDER` | `free` (local model, no credits). Set `xai` to use Grok API |
| `GROK_API_KEY` | needed only when `GROK_PROVIDER=xai` |
| `GROK_MODEL` | `local-free` (or `grok-4.6` for xAI) |
| `CORS_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000` |

Kafka is optional at runtime: if the broker is down, notifications and audit still persist to PostgreSQL; events are skipped with a warning.

## Docs

- [Architecture](docs/architecture.md)
- [API](docs/api.md)
- [Database schema](docs/schema.md)
