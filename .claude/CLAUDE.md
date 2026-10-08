# CLAUDE.md — Development Standards

> **Enterprise-grade always** — coding, design, architecture, security, testing. Maintainability, correctness, explicitness over convenience.

---

## Project Identity

Enterprise Multi-Tenant SaaS Platform (Java 25, Spring Boot 4). Generic, reusable foundation — domain projects (school, hospital, HR) plug in via `spring-boot-starter`.

Group ID: `com.altafjava.platform`
Module names: `core`, `domain`, `infrastructure`, `application`, `api`, `integration`, `spring-boot-starter` — no `platform-` prefix.

---

## Core Principles

SOLID, DRY, YAGNI, KISS, Fail Fast, Least Astonishment, Composition over Inheritance, Explicit over Implicit — always, without exception.

---

## Design Patterns

Use only when the problem demands it — no invented alternatives.

| Pattern | Location |
|---------|----------|
| Repository | `domain/{context}/repository/` |
| Factory Method | `Entity.create(...)` |
| Strategy | `JobExecutionStrategy`, `TenantResolver`, `ResourceAccessPolicy` |
| Observer/Event | `EventPublisher` + `@EventListener` |
| Decorator | `TenantContextPropagatingDecorator` |
| Chain of Responsibility | Tenant resolution fallback |
| Adapter | `BCryptPasswordEncoderAdapter`, repository adapters |
| Outbox | Event publishing — `TransactionalOutboxEventPublisher` (`infrastructure/.../event/outbox/`), backed by the `outbox_events` table |
| Saga | `SagaCoordinator` |

---

## Naming

- Classes/interfaces: `PascalCase` noun. No `I` prefix on interfaces.
- Methods: `camelCase` verb phrase. Variables: `camelCase` descriptive noun.
- Constants/enum values: `UPPER_SNAKE_CASE`.
- Packages: lowercase, **feature-based** (`subscription`, `tenant`) — never layer-based (`services`, `repositories`).
- Tests: `{ClassName}Test` / `{ClassName}IntegrationTest`; methods: `{scenario}_{expectedOutcome}`.
- No abbreviations except: `dto`, `id`, `url`, `jwt`, `api`.
- Database: `snake_case` plural tables; `idx_{table}_{col}`, `fk_{table}_{ref}`, `uq_{table}_{col}`.

---

## Architecture — Module Dependencies

```
core ← domain ← application ← api
                             ← infrastructure
```

- `core`: zero dependencies. Base entities, annotations, utilities only.
- `domain`: depends on `core`. Entities, repo interfaces, domain services, domain events. No JPA infrastructure, no HTTP.
- `application`: depends on `domain` + `core`. Use case orchestration. Never imports infrastructure implementations.
- `infrastructure`: implements interfaces from `domain`/`application`. No business logic.
- `api`: depends on `application` + `core`. Controllers, DTOs, mappers, validation only.

Never violate this — cross-layer needs go through an interface in the inner layer.

---

## Module Placement

| Creating | Place in |
|----------|---------|
| JPA entity | `domain/{context}/model/` |
| Repository interface | `domain/{context}/repository/` |
| Domain service (pure logic) | `domain/{context}/service/` |
| Use case / orchestration | `application/{context}/` |
| Saga | `application/saga/` |
| Scheduler job | `application/scheduler/` |
| Cross-domain event | `application/event/` |
| Domain event | `domain/{context}/event/` |
| REST controller | `api/controller/` |
| Request/response DTO | `api/dto/request/` or `api/dto/response/` |
| MapStruct mapper | `api/mapper/` |
| Spring `@Configuration` | `infrastructure/config/` |
| JPA repository impl | `infrastructure/persistence/` |
| External adapter (Stripe…) | `integration/{service}/` |
| Base entity, annotation, utility | `core/` |
| Spring auto-configuration | `spring-boot-starter/autoconfigure/` |

---

## Clean Code

- Methods: one thing, one abstraction level, max ~20 lines. No boolean params. Max 3 params (else introduce a parameter object).
- Classes: one reason to change. No `Utils`/`Helper` names — name for what they do. No static mutable state.
- Comments: WHY only, never WHAT. One line, max two. No TODO, no change history ("added for X fix", dates, before/after narration) — that belongs in the commit message.
- Conditionals: positive form (`isActive()` not `!isInactive()`), guard clauses over nested ifs, enums over `instanceof` chains.
- Errors: catch only what you can handle, never swallow silently, throw specific types, never use for control flow.

---

## Entity Design

- Extend the right base: `BaseEntity`, `TenantAwareEntity`, `SoftDeletableEntity`, or `ExposedEntity`.
- Every mutable entity: `@Version` for optimistic locking.
- Soft delete: `@SQLRestriction("deleted = false")` — never `@Where` (deprecated Hibernate 6).
- Enums: `EnumType.STRING` always.
- PII fields: `@Pii`.
- Constructors: `protected` no-arg for JPA, static factory `Entity.create(...)` for application use.
- Lombok: `@Getter` only. `equals`/`hashCode` on `id` only. Never `@Data`.
- Fetch: `LAZY` always. `@EntityGraph` at query time.
- Business logic lives on the entity (rich domain model) — not in services manipulating fields externally.

---

## Service Layer

- Constructor injection only — never `@Autowired` on fields or setters.
- `@Transactional` on application services only — not on domain services, not on controllers.
- `@Transactional(readOnly = true)` on all query-only methods.
- Application services return domain entities or value objects — never JPA proxies.
- Application services do not call other application services — extract shared logic to domain services.
- Domain services: no Spring annotations, no infrastructure dependencies.

---

## REST API

- URLs: `/api/v1/{resource}`, `/api/v1/{resource}/{id}`, `/api/v1/{resource}/{id}/{sub}`
- HTTP semantics: `POST` → 201, `GET` → 200/404, `PUT`/`PATCH` → 200, `DELETE` → 204
- Controllers: zero business logic — parse, delegate, respond.
- `@Valid` on every `@RequestBody` and complex query param.
- All list endpoints paginated — default 20, max 100. No unbounded `findAll()`.
- Return DTOs (Java records) — never JPA entities.
- Breaking changes → `/api/v2/`. Deprecated endpoints carry `Deprecation` + `Sunset` headers.

### API Versioning Strategy

| Trigger | Action |
|---------|--------|
| New field added to response | Non-breaking — add to v1 response DTO, update `unmappedTargetPolicy` if needed |
| Existing field renamed or removed | Breaking — introduce `/api/v2/` endpoint, keep v1 running with `Deprecation` + `Sunset` response headers |
| Request shape changed incompatibly | Breaking — new version required |
| New endpoint added | Non-breaking — add at current version |

- Deprecate for a minimum 6 months; every response from that endpoint carries `Deprecation: true` + `Sunset: <RFC 7231 date>`.
- Version the controller package too, not just the URL: `api/rest/v1/`, `api/rest/v2/`.
- v1 and v2 share the same application services — version differences are isolated to DTOs and mappers only.
- Document breaking changes in `CHANGELOG.md` under `Breaking Changes` before merging.

---

## DTOs

- Java records only.
- Request DTOs: Bean Validation on record components.
- Response DTOs: expose `publicId` (UUID) — never surrogate `id` (Long).
- Never reuse a request DTO as a response DTO.
- MapStruct mappers: `unmappedTargetPolicy = ReportingPolicy.ERROR`.
- Never map `id`, `tenantId`, `version`, `createdAt`, `updatedAt` from request DTOs.

---

## Exception Handling

- Throw specific: `TenantNotFoundException`, not `RuntimeException`.
- Business errors extend `BusinessException`; infrastructure failures extend `TechnicalException`.
- `GlobalExceptionHandler` in `api` covers all — no try-catch in controllers for already-handled exceptions.
- Error response shape: `code`, `message`, `traceId`, `timestamp` — no raw stack traces.

---

## Multi-Tenancy

- Tenant context set only by `TenantContextFilter`. Use `TenantContext.require()` to read it.
- All native SQL on tenant-scoped tables: `AND tenant_id = :tenantId`.
- Async: platform executor only — never raw `CompletableFuture.runAsync()`.
- Cache keys for tenant-scoped data: always `tenantAwareCacheKeyGenerator`.
- Every new multi-tenant feature requires a cross-tenant isolation test before merge.

---

## Security

- Access control: `@PreAuthorize` on controllers. Never manual role checks in services.
- Input validation: Bean Validation at API boundary only.
- Secrets: never hardcoded or raw `${VAR:default}` in YAML — via `SecretProvider` (`EnvironmentSecretProvider` dev/test, `VaultSecretProvider` staging/prod).
- Passwords: `BCryptPasswordEncoderAdapter` only.
- PII: `@Pii` annotation — never log, never expose raw.
- Queries: JPQL/named parameters only — no string concatenation, including identifiers (table/column names) built from user input.
- Non-dev profiles: `SecurityStartupValidator` fails startup if any required secret is absent or default.

---

## Configuration

Three tiers, each with its own store — never collapse into one:

| Tier | Examples | Store |
|------|----------|-------|
| Secrets | DB/Redis/RabbitMQ/ES credentials, JWT keys, encryption key, third-party API keys | `SecretProvider` (Vault in staging/prod) |
| Environment/infra tunables | Pool sizes, circuit-breaker/retry params, actuator exposure, cache TTL regions | Spring profile YAML, reviewed via PR |
| Tenant/business settings | Feature flags, per-tenant rate limits, branding, notification prefs | DB, key/JSON shape (see `FeatureFlag`), `tenantAwareCacheKeyGenerator` |

- Never add a new tenant- or business-facing value to `application.yml` — it belongs in tier 3.
- Tier-3 tables owned by `platform-saas` stay domain-generic — no school/hospital/HR-specific columns (extend via key/value, not new platform columns; domain apps own their own keys through `PlatformConfigurer`).

---

## Events

- Publish after `save()` within the same `@Transactional` method.
- Listeners: `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- Non-critical listeners: `@Async` — never block the request thread.
- Events are Java records: immutable, IDs and primitives only — never JPA entities.
- Removing or renaming event fields = major version bump.

---

## Testing

Three mandatory tiers — no tier substitutes for another.

- **Unit** (`{ClassName}Test`, JUnit 5 + Mockito, no Spring): domain invariants, service rules, DTO constraints. Never unit test controllers, repos, or security config.
- **Integration** (extend `BaseIntegrationTest`, real DB, no HTTP): queries, transactions, events, cache. Never mock repositories.
- **E2E** (extend `BaseRestAssuredTest`, full HTTP + real DB): status codes, response body shape, RBAC, tenant isolation.

Per-controller E2E minimum: happy path (status + body shape), unauthenticated → 401, wrong role → 403, tenant isolation.

Conventions: `TestDataFactory` for fixtures, `AuthenticationHelper` for tokens, Given/When/Then structure, `{scenario}_{expectedOutcome}` naming, no shared mutable state, `Awaitility` for async (never `Thread.sleep`), `createMockJwt()` + direct repo injection for setup.

Never: mock repos in integration tests, `@Disabled` placeholders, OpenAPI assertions, latency assertions.

---

## Logging

- SLF4J only; use `key=value`, include `tenantId` where available.
- `INFO` business, `WARN` expected, `ERROR` unexpected.
- Never log PII, secrets, tokens, passwords—even DEBUG.
- Use `@Slf4j`; no manual loggers. Exception: abstract bases needing subclass runtime logging may use `LoggerFactory.getLogger(getClass())` with a comment.
- `log` is reserved for the logger; rename conflicting vars/params.

---

## Performance

- No N+1 queries — `@EntityGraph` or `JOIN FETCH` for associations loaded in loops.
- New tables: indexes on `tenant_id`, `status`, `deleted`, `created_at`, all FKs.
- Non-critical writes (notifications, audit, analytics): `@Async`.
- Custom `@Query`: named fields only — never `SELECT *`.

---

## Database & Migrations

- All schema changes via Liquibase changeset — no direct DDL.
- Surrogate PKs: `BIGINT AUTO_INCREMENT`. External IDs: `public_id VARCHAR(36)` (UUID).
- **Dev**: edit changesets in-place; drop/recreate DB on checksum conflict.
- **Column order** in every `createTable` (and the matching entity fields): primary key, `public_id`, `tenant_id`, foreign keys, business fields, status/type fields, `created_at`/`created_by`/`updated_at`/`updated_by`, `deleted`/`deleted_at`/`deleted_by`, `version`. Generated helper columns come last.
- Soft-delete-safe unique key: a generated `active_*` column (NULL once `deleted = TRUE`) with the unique index on it — add it in the table's own changeset right after `createTable`.
- Dev: fold evolution (`addColumn`, renames, backfills) into the table's original `createTable` changeset instead of stacking alter changesets; seed data carries its final values directly.
- **Prod/staging**: new columns nullable or with default; renames/drops are multi-release; never modify existing changesets.

---

## Caching

- Tenant-scoped data: always `tenantAwareCacheKeyGenerator` — plain `@Cacheable` on tenant data is a data breach risk.
- Every cache region has a TTL in `ApplicationCacheConfig`.
- Mutation methods evict the relevant region.
- Never cache individual records by ID — cache aggregate read models and reference data.

---

## Subagent Discipline

Spawn subagents only for genuine parallelism or an open-ended multi-file search with no obvious path — not for single-file reads, symbol lookups, or targeted greps (use Bash/Read directly).
Explore agent: open-ended search only, never a known file path.

---

## Post-Edit Quality Gate

After every edit: `./gradlew compileJava compileTestJava` (large changes: `clean build`); fix all warnings before stopping. Run `publishToMavenLocal` after structural changes.

---

## Technology Reference

| Need | Use | Never |
|------|-----|-------|
| Date/time | `java.time.*` | `Date`, `Calendar` |
| Collections | Java standard `List`, `Map`, `Set` | Guava |
| Null safety | `Optional<T>` at method boundaries | `null` from public methods |
| Async | `@Async` with configured executor | `new Thread()`, bare `CompletableFuture.runAsync()` |
| HTTP client | `RestClient` (SB 3.2+) | `RestTemplate` |
| JSON | Jackson (auto-configured) | manual JSON string building |
| Passwords | `BCryptPasswordEncoderAdapter` | any other hasher |
| Encryption | `AesEncryptionService` | custom crypto |
| Secret retrieval | `SecretProvider` | raw `${VAR}` in YAML for anything sensitive |
| Event publishing | `EventPublisher` (core interface) | `ApplicationContext.publishEvent()` directly |
| Scheduling | `JobExecutionStrategy` + `@ScheduledJob` | `@Scheduled` |
| Distributed lock | `@SchedulerLock` (ShedLock) | `synchronized`, `ReentrantLock` across JVMs |
| External IDs | `UUID.randomUUID()` | sequential/timestamp IDs |

---

## Deliberate Non-Changes

Settled decisions — don't reopen without a new, concrete trigger.

- JPA entities stay mutable classes, never records — Hibernate needs mutable state for proxying/lazy-loading.
- Never seal `JobExecutionStrategy`, `TenantResolver`, or `ResourceAccessPolicy` — they're consumer extension points.
- Don't abstract Hibernate annotations (`@SQLRestriction`, `@Filter`, `@Cache`) behind a platform interface in `domain` — no ORM-swap plan.
- No `StructuredTaskScope` in production code — still a JDK preview feature (JEP 505).
- No blanket `@Async` → virtual-thread conversion — must be bound-sized and load-tested per executor.
- No JVM/GC/CDS tuning in-repo — `Dockerfile` (added Phase 0, 2026-08-17) builds `:app:bootJar` into a minimal, non-root runtime image; `JAVA_OPTS` is environment-overridable and empty by default rather than baked in. CI (`docker-build.yml`) builds and pushes it to `ghcr.io/<org>/school-saas` (tagged `:<sha>` and `:latest`) on every push to `main`; `k8s/base/` holds the corresponding deployment manifests (adapted from platform-saas's template).
- No second extension mechanism alongside `PlatformConfigurer`.

---

## Documentation Policy

- No new dated audit/status/readiness markdown (`*_AUDIT.md`, `*_ASSESSMENT.md`, `PHASE*.md`) — fixes go into code + tests, not point-in-time reports.
- Update living docs in place, never supersede with a new file: `README.md`, `DEVELOPER_GUIDE.md`, `ROADMAP.md`, `CONTRACTS.md`, `CONFIGURATION_STRATEGY.md`, `TESTING.md`, `CHANGELOG.md`, `MIGRATION.md`.

---

## Hard Rules — Never Violate

1. No business logic in controllers
2. No infrastructure imports (`EntityManager`, `RedisTemplate`, `RabbitTemplate`) in `domain` or `core`
3. No `@Autowired` on fields — constructor injection only
4. No `@Where` — use `@SQLRestriction`
5. No JPA entities returned from controllers
6. No schema change without a Liquibase changeset
7. No PII in logs
8. No hardcoded secrets or credentials
9. No unbounded list queries without pagination
10. No `@Cacheable` on tenant data without `tenantAwareCacheKeyGenerator`
11. No `FetchType.EAGER`
12. No `EnumType.ORDINAL`
13. No manual `TenantContext` setting outside `TenantContextFilter`
14. Never `git add`, `git commit` or `git push` without explicit user instruction
