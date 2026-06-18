# Platform Gaps Found During Phase 5

This document records every gap or friction point discovered while building the school domain project
on top of `com.altafjava.platform:spring-boot-starter`. Each gap is assessed for severity and tracked
against the platform roadmap.

---

## Gap 1 — No Test Starter Artifact

**Severity**: Medium  
**Discovered**: Phase 5 bootstrap — writing the first integration test  
**Phase impacted**: 5

### Description
The platform does not publish a `spring-boot-starter-test` (or equivalent) artifact. Domain projects
that want to write integration tests against the platform must manually replicate test infrastructure:
- `MariaDBContainer` setup with `DynamicPropertySource`
- Mock beans for `PaymentGateway` (`TestPaymentConfig`)
- Mock beans for Redis (`TestRedisConfig`)
- Quartz clustering disabled in tests (`org.quartz.jobStore.isClustered=false`)

Every domain project that uses the platform must duplicate this boilerplate.

### Expected Behaviour
The platform should publish `com.altafjava.platform:spring-boot-starter-test` containing:
- `PlatformIntegrationTestBase` (pre-configured `@SpringBootTest` base class with TestContainers)
- `TestRedisConfig` (in-memory Redis mock)
- `TestPaymentConfig` (stub `PaymentGateway`)
- A shared `application-test.yml` with sensible test defaults

### Workaround Applied
Copied `SchoolIntegrationTestBase`, `TestRedisConfig`, and `TestPaymentConfig` directly into the school
project under `app/src/test/java/com/altafjava/school/`. This works but means every new domain project
must duplicate the same setup.

### Platform Fix Required
- Create `spring-boot-starter-test` Gradle submodule in `platform-saas`
- Publish it alongside the main starter
- Update Phase 6 checklist to include the test artifact

---

## Gap 2 — TenantContext.setTenant Signature Not Documented

**Severity**: Low  
**Discovered**: Writing cross-tenant isolation tests  
**Phase impacted**: 5

### Description
`TenantContext` exposes multiple overloaded methods (`setTenant`, `setCurrentTenant`, `require`,
`getCurrentTenantId`). Their exact signatures and expected usage in test context vs production context
are not documented in `DEVELOPER_GUIDE.md` or any Javadoc. The distinction between setting tenant for
test purposes vs production (via `TenantContextFilter`) was unclear.

### Workaround Applied
Discovered signatures by reading source code and existing tests. Used `TenantContext.setTenant(Long, String)`
in tests.

### Platform Fix Required
- Add a section in `DEVELOPER_GUIDE.md` covering `TenantContext` API usage
- Add Javadoc to all `TenantContext` public methods explaining threading and lifecycle expectations

---

## Gap 3 — SchoolTenantProvisioningListener Needs Manual TenantContext Reset

**Severity**: Medium  
**Discovered**: Implementing `SchoolTenantProvisioningListener.onTenantCreated`  
**Phase impacted**: 5

### Description
The platform's `TenantContextPropagatingDecorator` propagates the caller's `TenantContext` into async
threads. However, at the point the `TenantCreatedEvent` fires, the caller context is the *system context*
(no tenant set) rather than the new tenant's context. As a result, `onTenantCreated` must manually call
`TenantContext.setTenant(event.tenantId(), null)` to set the new tenant before seeding, and `TenantContext.clear()`
in a `finally` block.

This is an easy mistake to make and not mentioned in `EVENTS.md`.

### Workaround Applied
Manually called `TenantContext.setTenant` at the start and `TenantContext.clear()` in a `finally` block
inside `onTenantCreated`.

### Platform Fix Required
- Document in `EVENTS.md` under `TenantCreatedEvent`: "The listener receives this event without an active
  tenant context. The listener is responsible for setting and clearing `TenantContext` if it needs to
  perform tenant-scoped database operations."
- Consider whether the platform's event publishing infrastructure should set the new tenant's context
  automatically before calling `@TransactionalEventListener` handlers for `TenantCreatedEvent`.

---

## Summary

| # | Gap | Severity | Status |
|---|-----|----------|--------|
| 1 | No test starter artifact | Medium | Open — requires Phase 6 platform fix |
| 2 | TenantContext API undocumented | Low | Open — documentation update |
| 3 | TenantCreatedEvent missing context guidance | Medium | Open — EVENTS.md update |

All three gaps are documentation or packaging concerns — no platform behavior changes were required to
complete Phase 5. The school project was built to completion without modifying any platform code.
