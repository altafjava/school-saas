# CLAUDE.md — Development Standards

> **Enterprise-grade always.** Every decision — coding, design, architecture, security, testing — must meet enterprise production standards. Choose maintainability, correctness, and explicitness over convenience. When two approaches work, pick the one a senior engineer would be proud to ship.

---

## Project Identity

Enterprise Multi-Tenant SaaS Platform (Java 21, Spring Boot 3). Generic, reusable foundation — domain projects (school, hospital, HR) plug in via `spring-boot-starter`.

Group ID: `com.altafjava.platform`
Module names: `core`, `domain`, `infrastructure`, `application`, `api`, `integration`, `spring-boot-starter` — no `platform-` prefix.

---

## Core Principles

SOLID, DRY, YAGNI, KISS, Fail Fast, Least Astonishment, Composition over Inheritance, Explicit over Implicit — always, without exception.

---

## Design Patterns

Use only when the problem demands it. No alternatives.

| Pattern | Location |
|---------|----------|
| Repository | `domain/{context}/repository/` |
| Factory Method | `Entity.create(...)` |
| Strategy | `JobExecutionStrategy`, `TenantResolver`, `ResourceAccessPolicy` |
| Observer/Event | `EventPublisher` + `@EventListener` |
| Decorator | `TenantContextPropagatingDecorator` |
| Chain of Responsibility | Tenant resolution fallback |
| Adapter | `BCryptPasswordEncoderAdapter`, repository adapters |
| Outbox | Event publishing |
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

**Never violate this.** Need to cross layers? Introduce an interface in the inner layer.

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
- Comments: WHY only, never WHAT. No TODO in production code.
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
- Secrets: environment variables only — never hardcoded, never in `application.yml`.
- Passwords: `BCryptPasswordEncoderAdapter` only.
- PII: `@Pii` annotation — never log, never expose raw.
- Queries: JPQL/named parameters only — no string concatenation.
- Non-dev profiles: fail startup if `JWT_SECRET` absent or default.

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

- SLF4J only. Structured `key=value` pairs: `tenant={} plan={}`.
- Levels: `INFO` business events, `WARN` expected failures, `ERROR` unexpected failures.
- Never log PII, secrets, tokens, or passwords — even at DEBUG.
- Always include `tenantId` where available.

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
- **Prod/staging**: new columns nullable or with default; renames/drops are multi-release; never modify existing changesets.

---

## Caching

- Tenant-scoped data: always `tenantAwareCacheKeyGenerator` — plain `@Cacheable` on tenant data is a data breach risk.
- Every cache region has a TTL in `ApplicationCacheConfig`.
- Mutation methods evict the relevant region.
- Never cache individual records by ID — cache aggregate read models and reference data.

---

## Subagent Discipline

Spawn subagents only when: (1) task requires genuine parallelism, or (2) open-ended search spans many files with no obvious path.
For single-file reads, symbol lookups, or targeted greps — use Bash/Read directly.
Explore agent: only for open-ended "find X across the codebase" — never for a known file path.

---

## Post-Edit Quality Gate

After every edit: `./gradlew compileJava compileTestJava`. Large changes: `./gradlew clean build`. Fix all warnings before stopping.
Also run: `./gradlew publishToMavenLocal` after structural changes.

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
| Event publishing | `EventPublisher` (core interface) | `ApplicationContext.publishEvent()` directly |
| Scheduling | `JobExecutionStrategy` + `@ScheduledJob` | `@Scheduled` |
| Distributed lock | `@SchedulerLock` (ShedLock) | `synchronized`, `ReentrantLock` across JVMs |
| External IDs | `UUID.randomUUID()` | sequential/timestamp IDs |

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
14. Never `git commit` or `git push` without explicit user instruction
