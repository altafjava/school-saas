# Load Testing Guide

Load tests are written with **Gatling 3** (Java API) and live in
`platform-saas/spring-boot-starter/src/gatling/java/com/altafjava/platform/performance/`.

All Gatling commands are run **from the `platform-saas` directory** — the simulations are part of
the platform, but they require a running domain app (school-saas) as the target.

---

## Fresh-start checklist (new developer or wiped DB)

Follow these steps in order before running any simulation.

### 1. Start Docker infrastructure

```bash
# In school-saas/
docker compose up -d
docker compose ps          # wait until all services show "healthy" or "running"
```

### 2. Start the application

```bash
# In school-saas/
./gradlew :app:bootRun
```

Wait for `Started SchoolApplication` in the console. Liquibase runs on first boot and creates all
tables (you will see `Previously run: 0, Run: 110` on a fresh DB).

### 3. Register a tenant

> **Important:** `planId` is a number, not a string. Seeded plan IDs:
> `1` = Free Tier, `2` = Basic ($29), `3` = Professional ($99), `4` = Enterprise ($299)

> **Do NOT send `X-Tenant-Id` on this call.** The tenant doesn't exist yet — the filter will reject
> the request before registration even starts.

```bash
curl -X POST http://localhost:8080/api/v1/tenants/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo School",
    "subdomain": "demo",
    "planId": 2,
    "adminEmail": "admin@demo.com",
    "adminPassword": "Admin@123!",
    "currency": "USD"
  }'
```

Expected: `201 Created`. The response body includes `"id": "1"` — that is your `X-Tenant-Id`.

If you get `409 DUPLICATE_SUBDOMAIN`, the tenant already exists (common on a re-run). Skip to Step 4.

### 4. Acquire a JWT token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{"email": "admin@demo.com", "password": "Admin@123!"}'
```

Expected: `200 OK` with `accessToken` in the response body. Copy it — you will pass it as
`-DbearerToken=<token>` to simulations 2–5.

> **If you get `429 Too Many Requests`:** the login rate limiter (5 logins/minute per IP) has
> triggered. Wait 60 seconds and try again. This is the rate limiter working correctly.

---

## Common errors and fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `planId must not be null` | Sending `"plan": "BASIC"` instead of `"planId": 2` | Use `planId` with a number |
| `No tenant in context` on login | Tenant not registered yet, or wrong `X-Tenant-Id` | Register the tenant first; use `X-Tenant-Id: 1` |
| `409 DUPLICATE_SUBDOMAIN` | Tenant with same subdomain already exists | Skip registration — go straight to login |
| `429 Too Many Requests` on login | Rate limiter: 5 logins/min per IP | Wait 60s and retry |
| `IllegalStateException: TenantApiLoadSimulation requires a valid JWT` | `-DbearerToken=` not passed | Acquire a token (Step 4) and pass it |
| Simulation fails with `percentage of failed events` | See simulation-specific notes below | |

---

## Running simulations

All commands below are run from the **`platform-saas/`** directory.

### Simulation 1 — Auth load (register + login + refresh)

Tests the full auth flow under concurrent load. Each virtual user self-registers, so no token is needed.

```bash
cd ../platform-saas

./gradlew :spring-boot-starter:gatlingRun \
  -DsimulationClass=com.altafjava.platform.performance.AuthLoadSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DtargetUsers=20 \
  -DrampSeconds=30
```

**What it does:** Ramps 20 users over 30 seconds. Each user: registers → logs in → refreshes token.

**About 429 on login:** The rate limiter allows 5 logins/minute per IP. Under local load all
requests come from `127.0.0.1`, so the 6th+ concurrent login returns 429. The simulation accepts
429 as a non-failure status because rate limiting is expected and intentional — the assertion
threshold is on true errors (5xx), not rate-limited requests.

**Full-scale (CI/staging):** `-DtargetUsers=500 -DrampSeconds=60`

---

### Simulation 2 — Tenant API load (reads + writes)

Tests the core tenant API (users, subscriptions) with a realistic 70% read / 30% write mix.
**Requires a valid JWT token** (Step 4 above).

```bash
./gradlew :spring-boot-starter:gatlingRun \
  -DsimulationClass=com.altafjava.platform.performance.TenantApiLoadSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DbearerToken=<paste-accessToken-here>
```

**What it does:** 10 req/sec for 30s (local defaults). Reads: `GET /api/v1/tenants/current`,
`GET /api/v1/users`, `GET /api/v1/subscriptions/current`. Writes: create/update/delete user.

**If you omit `-DbearerToken=`:** all authenticated requests return 401 and the simulation
fails its error-rate assertion — by design, so the failure is clear rather than a crash.

**Full-scale (CI/staging):** `-DusersPerSec=200 -DdurationSeconds=60`

---

### Simulation 3 — Multi-tenant concurrent isolation

Tests that tenants operating simultaneously do not bleed data into each other.

```bash
./gradlew :spring-boot-starter:gatlingRun \
  -DsimulationClass=com.altafjava.platform.performance.MultiTenantConcurrentSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DtenantCount=3 \
  -DusersPerTenant=5 \
  -DrampSeconds=20 \
  -DbearerToken=<paste-accessToken-here>
```

**Full-scale (CI/staging):** `-DtenantCount=50 -DusersPerTenant=20`

---

### Simulation 4 — Scheduler job throughput

Tests the Quartz job admin API under concurrent load.

```bash
./gradlew :spring-boot-starter:gatlingRun \
  -DsimulationClass=com.altafjava.platform.performance.SchedulerJobThroughputSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DconcurrentJobs=10 \
  -DadminToken=<paste-accessToken-here>
```

**Full-scale (CI/staging):** `-DconcurrentJobs=100`

---

### Simulation 5 — Elasticsearch search load

Tests cross-tenant search with circuit breaker validation.

```bash
./gradlew :spring-boot-starter:gatlingRun \
  -DsimulationClass=com.altafjava.platform.performance.SearchLoadSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DconcurrentUsers=10 \
  -DrampSeconds=20 \
  -DbearerToken=<paste-accessToken-here>
```

Returns `200` (live ES), `200` (DB fallback), or `503` (circuit open) — all are valid responses.

**Full-scale (CI/staging):** `-DconcurrentUsers=100 -DrampSeconds=30`

---

## Recommended local vs full-scale parameters

| Simulation | Local (laptop) | Full-scale (CI/staging) |
|-----------|---------------|------------------------|
| Auth | `targetUsers=20, rampSeconds=30` | `targetUsers=500, rampSeconds=60` |
| Tenant API | `usersPerSec=10, durationSeconds=30` | `usersPerSec=200, durationSeconds=60` |
| Multi-tenant | `tenantCount=3, usersPerTenant=5` | `tenantCount=50, usersPerTenant=20` |
| Scheduler | `concurrentJobs=10` | `concurrentJobs=100` |
| Search | `concurrentUsers=10, rampSeconds=20` | `concurrentUsers=100, rampSeconds=30` |

---

## Reading the HTML report

After each run Gatling writes a self-contained HTML report:

```
platform-saas/spring-boot-starter/build/reports/gatling/<SimulationName>-<timestamp>/index.html
```

Open it in a browser. Key sections:

| Section | What to look for |
|---------|-----------------|
| **Response time percentiles** | P95 < 500ms, P99 < 2s (platform baseline) |
| **Requests/sec** | Sustained throughput over the test duration |
| **Error rate** | Must be < 0.1% for a passing run |
| **Active users over time** | Ramp shape — confirms injection worked as expected |

A run **fails** (non-zero Gradle exit code) if any assertion threshold is breached.

---

## Resetting to a clean state

If you need a completely fresh start (empty DB, no tenants):

```bash
# In school-saas/ — stop everything and delete all data volumes
docker compose down -v

# Restart infrastructure
docker compose up -d

# Restart the app — Liquibase re-creates all tables from scratch
./gradlew :app:bootRun
```

Then repeat the fresh-start checklist from Step 3.

---

## Simulating resilience scenarios manually

| Scenario | How to trigger | Expected behaviour |
|---------|---------------|--------------------|
| Elasticsearch down | `docker stop school_elasticsearch` mid-run | Search returns `200` from DB fallback; circuit opens after 3 failures |
| RabbitMQ down | `docker stop school_rabbitmq` mid-run | Publish calls throw `AmqpConnectException`; app stays up |
| DB connection burst | Run Auth + Tenant API simulations simultaneously | HikariCP queues requests; none dropped; pool recovers after burst |

Restart the container when done: `docker start school_elasticsearch`
