# school-saas

A domain module built on top of the **platform-saas** multi-tenant SaaS foundation. Handles students, teachers, grades, fees, and attendance for school institutions.

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java | 21+ | [sdkman.io](https://sdkman.io) — `sdk install java 21` |
| Docker Desktop | Latest | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) |

Gradle does not need to be installed — the repo ships with `./gradlew`.

---

## Platform Dependency

This project consumes the platform as a published artifact — it does **not** reference the platform source directly.

| Environment | How the artifact is resolved |
|-------------|------------------------------|
| Local development | `~/.m2` (local Maven cache) — publish from platform-saas first |
| CI / CD | GitHub Packages — resolved automatically using `GITHUB_ACTOR` + `GITHUB_TOKEN` |

**One-time local setup** — run this once in the `platform-saas` directory whenever the platform version changes:

```bash
cd ../platform-saas
./gradlew publishToMavenLocal
```

After that, `./gradlew build` in this project resolves `com.altafjava.platform:*:1.0.0-SNAPSHOT` from your local Maven cache automatically.

> **Re-publish whenever platform source changes.** This project reads the artifact from `~/.m2` — it will not pick up platform code changes until `publishToMavenLocal` is re-run in `platform-saas`.

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Could not find com.altafjava.platform:platform-bom:…` | Never published locally | Run `publishToMavenLocal` in `platform-saas` |
| App starts with old behaviour after a platform change | Stale artifact in `~/.m2` | Re-run `publishToMavenLocal` in `platform-saas` |
| Bean conflict / startup error after platform update | Old JAR still cached | Re-run `publishToMavenLocal` then restart the app |

---

## Local Development Setup

### 1. Start all infrastructure

```bash
docker compose up -d
```

This starts all backing services (MariaDB, Redis, Elasticsearch, RabbitMQ, MinIO, Zipkin, Prometheus, Grafana) in the background. On first run, Docker pulls the images — takes a few minutes.

Wait for the essentials to be healthy before starting the app:

```bash
docker compose ps
```

All services should show `healthy` or `running`.

### 2. Run the application

```bash
./gradlew :app:bootRun
```

The `dev` profile is active by default (set in `application.yml`). The app starts on `http://localhost:8080`.

### 3. Verify startup

- Swagger UI: `http://localhost:8080/swagger-ui`
- Health check: `http://localhost:8080/actuator/health`

---

## Infrastructure Services

| Service | URL | Credentials | Purpose |
|---------|-----|-------------|---------|
| MariaDB | `localhost:3306` | `root` / `mysql` | Primary database |
| Redis | `localhost:6379` | — | Cache, sessions, distributed locks |
| Elasticsearch | `http://localhost:9200` | — | Full-text search |
| RabbitMQ | `localhost:5672` | `guest` / `guest` | Async messaging |
| RabbitMQ UI | `http://localhost:15672` | `guest` / `guest` | Broker management |
| MinIO API | `http://localhost:9000` | `minioadmin` / `minioadmin` | S3-compatible object storage |
| MinIO Console | `http://localhost:9001` | `minioadmin` / `minioadmin` | Storage management UI |
| Zipkin | `http://localhost:9411` | — | Distributed tracing |
| Prometheus | `http://localhost:9090` | — | Metrics collection |
| Grafana | `http://localhost:3000` | `admin` / `admin` | Metrics dashboards |

---

## Useful Docker Compose Commands

```bash
# Start all services
docker compose up -d

# Stop all services (data is preserved in named volumes)
docker compose down

# Stop and delete all data volumes (full reset)
docker compose down -v

# View logs for a specific service
docker compose logs -f mariadb

# Restart a single service
docker compose restart redis

# Start only the essentials (skip observability stack)
docker compose up -d mariadb redis elasticsearch
```

---

## Running Tests

Tests use TestContainers and spin up their own isolated MariaDB and RabbitMQ containers automatically — no manual broker required and no conflict with the running dev containers.

```bash
./gradlew test
```

---

## Load Testing

With the app and all Docker containers running, Gatling load tests can be executed against this app from the `platform-saas` directory.

Quick start — auth load test with local-safe parameters:

```bash
# 1. Register a tenant (one-time) — planId is a number (2=Basic), not a plan name string
curl -X POST http://localhost:8080/api/v1/tenants/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo School","subdomain":"demo","planId":2,"adminEmail":"admin@demo.com","adminPassword":"Admin@123!","currency":"USD"}'

# 2. Run a simulation (from platform-saas/)
cd ../platform-saas
./gradlew :spring-boot-starter:gatlingRun \
  -DsimulationClass=com.altafjava.platform.performance.AuthLoadSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DtargetUsers=20 \
  -DrampSeconds=30
```

For all 5 simulations, token acquisition, report reading, and resilience scenario testing, see **[LOAD_TESTING.md](LOAD_TESTING.md)**.

---

## Environment Variables

All values below have safe local defaults. Override via environment variables or a `.env` file in the project root when you need different values.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MariaDB host |
| `DB_PORT` | `3306` | MariaDB port |
| `DB_NAME` | `school_saas` | Database name |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | `mysql` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `ELASTICSEARCH_URIS` | `http://localhost:9200` | Elasticsearch URI |
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` | Zipkin spans endpoint |
| `STRIPE_API_KEY` | — | Stripe secret key (required for payment flows) |
| `STRIPE_WEBHOOK_SECRET` | — | Stripe webhook signing secret |
| `SENTRY_DSN` | — | Sentry DSN (optional; errors log locally if unset) |
| `JWT_SECRET` | — | JWT signing secret (required on non-dev profiles) |

---

## Module Structure

```
school-saas/
├── domain/       Entity models, repository interfaces, domain services
├── application/  Use case orchestration, event listeners, sagas, schedulers
├── api/          REST controllers, DTOs, mappers, input validation
└── app/          Spring Boot entry point, resources, configuration
```

Dependency direction: `app → api → application → domain`. The `app` module pulls in the `platform-saas` spring-boot-starter which auto-configures all infrastructure (multi-tenancy, security, messaging, caching, payments, scheduling).
