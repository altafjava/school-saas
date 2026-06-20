# CLAUDE.md — Development Standards for Claude Code

Follow every rule in this file consistently across all sessions. These are non-negotiable constraints, not suggestions.

---

## Project Identity

This is an **Enterprise Multi-Tenant SaaS Platform** (Java 21, Spring Boot 3). It is a generic, reusable foundation — not a school application. Any domain project (school, hospital, library, HR) plugs into it via the `spring-boot-starter` module. See `PLATFORM_ROADMAP.md` for the full vision.

Group ID: `com.yourorg.platform`
Module naming: simple names (`core`, `domain`, `infrastructure`, `application`, `api`, `integration`, `spring-boot-starter`) — no `platform-` prefix inside the project.

---

## General Programming Principles

These apply everywhere, always — not just in this project.

### SOLID

- **Single Responsibility** — Every class does one thing. If you need "and" to describe it, split it.
- **Open/Closed** — Extend via interfaces and composition, not by modifying existing classes.
- **Liskov Substitution** — Subtypes must be substitutable for their base type without changing behavior.
- **Interface Segregation** — Small, focused interfaces. A class should not be forced to implement methods it does not use.
- **Dependency Inversion** — Depend on abstractions, not concretions. High-level modules never import low-level implementations.

### Other Core Principles

- **DRY** — Every piece of knowledge has a single, authoritative representation. Duplication is a maintenance debt.
- **YAGNI** — Do not build what is not currently needed. Abstractions built for hypothetical future requirements rot.
- **KISS** — The simplest solution that satisfies the requirement is correct. Complexity must be justified.
- **Fail Fast** — Validate preconditions at the entry point. Surface errors early with clear messages rather than propagating bad state.
- **Principle of Least Astonishment** — Code should behave the way a reader expects. Surprising behavior is a design flaw.
- **Composition over Inheritance** — Prefer composing behavior through interfaces and delegation over deep inheritance hierarchies.
- **Explicit over Implicit** — Clarity beats cleverness. Magic is a liability.

---

## Design Patterns in Use

Use these patterns consistently. Do not invent alternatives for problems already solved.

| Pattern | Where Used | Purpose |
|---------|-----------|---------|
| Repository | `domain/{context}/repository/` | Data access abstraction — domain never touches JPA directly |
| Factory Method | Domain entities (`Entity.create(...)`) | Controlled construction, enforces invariants |
| Strategy | `JobExecutionStrategy`, `TenantResolver`, `ResourceAccessPolicy` | Swappable behavior without conditionals |
| Observer / Event | `EventPublisher` + `@EventListener` | Decoupled side effects after domain changes |
| Decorator | `TenantContextPropagatingDecorator` | Wraps async tasks to propagate context |
| Chain of Responsibility | Tenant resolution fallback chain | Sequential resolution strategies |
| Template Method | Base scheduler jobs, base integration tests | Common skeleton with overridable steps |
| Adapter | `BCryptPasswordEncoderAdapter`, repository adapters | Wrap external interfaces to match internal contracts |
| Outbox (Transactional Messaging) | Event publishing | Ensure events are persisted before being dispatched |
| Saga | `SagaCoordinator` | Distributed transaction management with compensation |

Do not use a pattern just because it is a known pattern. Use it because the problem demands it.

---

## Naming Conventions

Consistency in naming reduces cognitive load. Follow these without exception.

### Java

| Element | Convention | Rule |
|---------|-----------|------|
| Class | `PascalCase` | Noun or noun phrase. Describes what it IS, not what it does |
| Interface | `PascalCase` | Noun or adjective (`Auditable`, `TenantResolver`). No `I` prefix |
| Method | `camelCase` | Verb or verb phrase. Describes what it DOES |
| Variable | `camelCase` | Descriptive noun. No single-letter names except loop counters |
| Constant | `UPPER_SNAKE_CASE` | All caps, underscores |
| Enum type | `PascalCase` | Noun — what it classifies |
| Enum value | `UPPER_SNAKE_CASE` | Specific state or category |
| Package | `lowercase` | Feature-based, not layer-based (`subscription`, not `services`) |
| Test class | `{SubjectClass}Test` / `{SubjectClass}IntegrationTest` | Mirrors the class under test |
| Test method | `{scenario}_{expectedOutcome}` | Reads as a sentence describing the assertion |

### No Abbreviations

Never abbreviate variable or field names. `tenantId` not `tid`. `subscriptionRepository` not `subRepo`. `emailAddress` not `email` if the full name adds clarity. Abbreviations are acceptable only for universally understood acronyms (`dto`, `id`, `url`, `jwt`, `api`).

### Database

| Element | Convention |
|---------|-----------|
| Table | `snake_case`, plural (`subscription_plans`, `audit_logs`) |
| Column | `snake_case` (`tenant_id`, `created_at`) |
| Index | `idx_{table}_{column(s)}` (`idx_users_tenant_id`) |
| Foreign key | `fk_{table}_{referenced_table}` |
| Unique constraint | `uq_{table}_{column(s)}` |

### Packages

Organize by **feature/domain**, never by layer:

```
com.yourorg.platform.subscription   ← all subscription code: entity, repo, service
com.yourorg.platform.tenant         ← all tenant code
com.yourorg.platform.billing        ← all billing code
```

Not:
```
com.yourorg.platform.service        ← WRONG: mixes unrelated concerns
com.yourorg.platform.repository     ← WRONG: layer-based grouping
```

---

## Architecture Rules

### Module Dependency Direction — Never Violate

```
core
 ↑
domain
 ↑
application
 ↑           ↑
api       infrastructure
```

- `core` has zero dependencies on other modules
- `domain` depends only on `core`
- `application` depends on `domain` and `core` — never on `infrastructure` implementations
- `infrastructure` implements interfaces from `domain` and `application`
- `api` depends on `application` and `core`

If you find yourself needing to violate this, introduce an interface in the inner layer and implement it in the outer layer.

### Layer Responsibilities

| Layer | Allowed | Forbidden |
|-------|---------|----------|
| `core` | Base entities, annotations, abstractions, utilities | Any Spring annotation except `@Component` on utilities |
| `domain` | Entities, repository interfaces, domain services, domain events | JPA infrastructure code, HTTP, messaging |
| `application` | Use case orchestration, saga coordination, event publishing | Direct HTTP context, direct DB access |
| `infrastructure` | JPA repos, Redis, RabbitMQ, S3, email — all technical impls | Business logic |
| `api` | Controllers, DTOs, mappers, input validation | Business logic, direct DB access |

### Boundaries Between Layers

- Application services call domain repositories (interfaces) — never `EntityManager` directly
- Controllers call application services — never domain repositories or services directly
- Infrastructure implements domain/application interfaces — never the other way around
- Domain entities carry business logic (behavior) — services orchestrate, entities decide

---

## Clean Code Rules

### Methods

- A method does **one thing** — single level of abstraction per method
- Maximum ~20 lines before extracting a helper. If it doesn't fit on a screen, it is too large
- No boolean parameters — they are a sign the method does two things; split it
- No more than 3 parameters — if you need more, introduce a parameter object
- Verb names: `activateSubscription`, `findActiveByTenantId`, `validatePaymentMethod`

### Classes

- A class has **one reason to change** (Single Responsibility)
- No utility/helper classes named `Utils` or `Helper` — name them for what they do (`TenantSchemaResolver`, not `TenantUtils`)
- Prefer small classes over large ones — a 500-line class is a smell
- No static mutable state

### Comments

- Comments explain **WHY**, never WHAT — the code explains what
- A comment that restates the code in English is noise — delete it
- Acceptable comments: non-obvious constraints, external references (bug IDs, RFC numbers), performance workarounds
- Never leave TODO comments in production code — resolve them before merging

### Conditionals

- Avoid negated conditions — `if (isActive())` over `if (!isInactive())`
- Replace complex conditionals with well-named boolean methods
- Avoid deeply nested ifs — use guard clauses (early return for invalid cases)
- Enum-based switching is preferable to `instanceof` chains

### Error Handling

- Only catch exceptions you can meaningfully handle
- Never swallow exceptions silently — at minimum, log them
- Throw the most specific exception type available
- Checked exceptions for recoverable business errors; unchecked for programmer errors and infrastructure failures
- Never use exceptions for control flow

---

## Refactoring Principles

When touching existing code, leave it better than you found it (Boy Scout Rule) — but scope the cleanup to what is relevant to your change.

- **Extract Method** when code needs a comment to be understood — the extracted method name replaces the comment
- **Rename** when the name no longer reflects the intent — names should always be honest
- **Extract Interface** when a class is used in ways that suggest multiple responsibilities
- **Replace Conditionals with Polymorphism** when a switch/if-else dispatches on type
- **Introduce Parameter Object** when 3+ related parameters travel together across multiple methods
- **Strangler Fig** for large refactors — wrap the old code, replace incrementally, delete the original

Never refactor and add features in the same commit. Separate structural changes from behavioral changes.

---

## Module Placement Rules

When creating something new, ask: which layer does this belong to?

| Creating | Place in |
|----------|---------|
| JPA entity | `domain/{context}/model/` |
| Repository interface | `domain/{context}/repository/` |
| Domain service (pure logic) | `domain/{context}/service/` |
| Use case / orchestration service | `application/{context}/` |
| Saga coordinator | `application/saga/` |
| Scheduler job | `application/scheduler/` |
| Event class | `application/event/` (if cross-domain) or `domain/{context}/event/` |
| REST controller | `api/controller/` |
| Request / response DTO | `api/dto/request/` or `api/dto/response/` |
| MapStruct mapper | `api/mapper/` |
| Spring `@Configuration` class | `infrastructure/config/` |
| JPA repository implementation | `infrastructure/persistence/` |
| External service adapter (Stripe, etc.) | `integration/{service}/` |
| Base entity, annotation, utility | `core/` |
| Spring auto-configuration | `spring-boot-starter/autoconfigure/` |

---

## Entity Design Rules

- Extend the correct base class: `BaseEntity` (system), `TenantAwareEntity` (tenant-scoped), `SoftDeletableEntity` (soft delete), `ExposedEntity` (public UUID)
- Every mutable entity requires `@Version` for optimistic locking
- Use `@SQLRestriction("deleted = false")` — never `@Where` (deprecated in Hibernate 6)
- Use `EnumType.STRING` — never `EnumType.ORDINAL`
- PII fields (name, email, phone, address) annotated with `@Pii`
- No public no-arg constructors — `protected` for JPA, static factory methods for creation
- No Lombok `@Data` — use `@Getter` only; `equals`/`hashCode` based on `id` only
- Never use `FetchType.EAGER` — always `LAZY`, use `@EntityGraph` at query time
- Business logic belongs on the entity (rich domain model), not in a service that manually manipulates fields

---

## Service Layer Rules

- **Constructor injection only** — no `@Autowired` on fields or setters
- `@Transactional` on application services — not on domain services, not on controllers
- `@Transactional(readOnly = true)` on all query-only methods
- Application services return domain entities or value objects — never JPA proxies
- Application services do not call other application services — extract shared logic to domain services
- Domain services have no Spring annotations and no infrastructure dependencies — pure business logic only

---

## REST API Rules

- URL pattern: `/api/v1/{resource}` (collection), `/api/v1/{resource}/{id}` (single), `/api/v1/{resource}/{id}/{sub}` (nested)
- HTTP semantics: `POST` → 201, `GET` → 200 or 404, `PUT`/`PATCH` → 200, `DELETE` → 204
- Controllers contain zero business logic — parse, delegate, respond
- Every `@RequestBody` and complex query param has `@Valid`
- All list endpoints are paginated — no unbounded `findAll()`, default page 20, max 100
- Return DTOs (Java records), never JPA entities from controllers
- Breaking API changes go to `/api/v2/` — never modify `/api/v1/` behavior
- Deprecated endpoints carry `Deprecation` and `Sunset` response headers

---

## DTO Rules

- Use Java **records** for all DTOs — not classes
- Request DTOs: Bean Validation annotations on record components
- Response DTOs: expose `publicId` (UUID) externally, never surrogate `id` (Long)
- Never reuse a request DTO as a response DTO — different purposes, different shapes
- MapStruct mappers with `unmappedTargetPolicy = ReportingPolicy.ERROR` — unmapped fields are a compile error
- Never map `id`, `tenantId`, `version`, `createdAt`, `updatedAt` from request DTOs — system-managed fields

---

## Exception Handling Rules

- Throw the most specific exception type: `TenantNotFoundException`, not `RuntimeException`
- Business exceptions extend `BusinessException` (known, expected errors)
- Infrastructure exceptions extend `TechnicalException` (unexpected failures)
- `GlobalExceptionHandler` in `api` module handles all exceptions — do not add try-catch in controllers for exceptions already covered
- Never return raw exception messages or stack traces in API responses
- Error response shape: machine-readable `code`, human-readable `message`, `traceId`, `timestamp`

---

## Multi-Tenancy Rules

- Tenant context is set by `TenantContextFilter` — never set it manually in production code
- Use `TenantContext.require()` to read the current tenant; it throws if context is missing
- Never write a native SQL query on a tenant-scoped table without `AND tenant_id = :tenantId`
- Async methods automatically inherit tenant context via the configured `TaskDecorator` — only use the platform's configured executor, never raw `CompletableFuture.runAsync()`
- Every new multi-tenant feature requires an explicit cross-tenant isolation test before merging
- Cache keys for tenant-scoped data always use `tenantAwareCacheKeyGenerator`

---

## Security Rules

- Access control via `@PreAuthorize` on controllers — never manual role checks inside services
- Input validation at the API boundary via Bean Validation — not inside service methods
- All secrets from environment variables — no hardcoded values, no secrets in `application.yml`
- Passwords via `BCryptPasswordEncoderAdapter` — nothing else
- PII fields use `@Pii` annotation — never log, never expose raw in responses
- SQL via JPQL/named parameters only — no string concatenation in queries
- Non-dev profiles must fail startup if `JWT_SECRET` is absent or set to the default value

---

## Event-Driven Rules

- Publish events after `save()` within the same `@Transactional` method
- Use `@TransactionalEventListener(phase = AFTER_COMMIT)` — events fire only after the transaction commits
- Event listeners are `@Async` for non-critical side effects — never block the request thread
- Events are Java records — immutable, carry only IDs and primitive values, never JPA entities
- Platform-published events are a public API contract — removing or renaming fields requires a major version bump

---

## Testing Rules

- Test pyramid: many unit tests → fewer integration tests → few E2E tests
- Integration tests use TestContainers with a real database — never mock repositories
- Every new multi-tenant feature has a cross-tenant isolation test
- Use `TestDataFactory` for all fixture creation — never construct entities directly in tests
- Use `AuthenticationHelper` for JWT tokens in tests — never hardcode tokens
- Tests are independent — no shared mutable state between test methods
- Test method naming: `{scenario}_{expectedOutcome}` — reads as a sentence
- Structure: Given / When / Then — always, no exceptions

---

## Logging Rules

- SLF4J only — never `System.out.println` or `java.util.logging`
- Structured key=value parameters: `tenant={} plan={}` for ELK/Splunk compatibility
- Levels: `INFO` for business events, `WARN` for expected failures, `ERROR` for unexpected failures
- Never log PII — use `@Pii` annotation; the masking serializer handles redaction automatically
- Never log secrets, tokens, or passwords — even at DEBUG level
- Include `tenantId` in log messages wherever available for traceability

---

## Performance Rules

Before any change that touches data access:

- No N+1 queries — use `@EntityGraph` or `JOIN FETCH` for associations loaded in a loop
- All new tables have indexes on `tenant_id`, `status`, `deleted`, `created_at`, and all foreign keys
- List endpoints paginate — no unbounded queries
- Non-critical writes (notifications, analytics, audit logs) are async — never block the request thread
- Apply caching only to read-heavy, infrequently-changed data with explicit TTL
- `@Transactional(readOnly = true)` on all query-only methods — enables query optimizations
- Custom `@Query` selects named fields only — never `SELECT *`

---

## Database & Migration Rules

- Every schema change goes through a Liquibase changeset — no direct DDL
- New columns: nullable or with a default — never `NOT NULL` without a default on existing tables
- Column renames and drops are multi-release operations: add → migrate data → drop (separate PRs)
- Surrogate PKs are `BIGINT AUTO_INCREMENT`; external-facing IDs are `public_id VARCHAR(36)` (UUID)
- Migrations are backward-compatible — the new code and the old code must both work against the migrated schema

---

## Caching Rules

- Tenant-scoped cache entries always use `tenantAwareCacheKeyGenerator` — plain `@Cacheable` on tenant data is a data breach risk
- Every cacheable region has a TTL defined in `ApplicationCacheConfig`
- Mutation methods (`create`, `update`, `delete`) always evict the relevant cache region
- Never cache individual records fetched by ID — cache aggregate read models and reference data

---

## Post-Edit Quality Gate

After every edit, run ./gradlew compileJava compileTestJava and fix all warnings before completing the task. No warning is acceptable, regardless of type.

---

## What Claude Code Must Never Do

1. Put business logic in a controller
2. Import infrastructure classes (`EntityManager`, `RedisTemplate`, `RabbitTemplate`) into `domain` or `core`
3. Use `@Autowired` on fields — constructor injection only
4. Use `@Where` — use `@SQLRestriction` (Hibernate 6+)
5. Return JPA entities from controllers — always map to a DTO first
6. Drop or rename a column in the same release as the code change
7. Log PII data — use `@Pii` annotation instead
8. Hardcode any secret or credential
9. Write an unbounded list query without pagination
10. Use `@Cacheable` on tenant-scoped data without `tenantAwareCacheKeyGenerator`
11. Make a schema change without a Liquibase changeset
12. Set `TenantContext` manually in production code outside of `TenantContextFilter`
13. Use `FetchType.EAGER` on any association
14. Use `EnumType.ORDINAL`
15. Create an abstraction, utility, or pattern not required by the current task (YAGNI)
16. Run `git commit` or `git push` (or any destructive git command) — never commit or push to the repository without explicit instruction from the user

---

## Technology Quick Reference

| Need | Use | Never Use |
|------|-----|-----------|
| Date/time | `java.time.*` | `java.util.Date`, `Calendar` |
| Collections | Java standard (`List`, `Map`, `Set`) | Guava |
| Null safety | `Optional<T>` at method boundaries | `null` returns from public methods |
| Async | `@Async` with configured executor | Raw `new Thread()`, bare `CompletableFuture.runAsync()` |
| HTTP client | `RestClient` (Spring Boot 3.2+) | `RestTemplate` |
| JSON | Jackson (auto-configured) | Manual JSON string building |
| Passwords | `BCryptPasswordEncoderAdapter` | Any other hasher |
| Encryption | `AesEncryptionService` | Custom crypto |
| Event publishing | `EventPublisher` interface (`core`) | `ApplicationContext.publishEvent()` directly |
| Scheduling | `JobExecutionStrategy` + `@ScheduledJob` | `@Scheduled` |
| Distributed locking | `@SchedulerLock` (ShedLock) | `synchronized`, `ReentrantLock` across JVM instances |
| UUIDs | `UUID.randomUUID()` | Sequential or timestamp-based IDs for external exposure |
