# @school/api-client

Typed TypeScript client for the school-saas REST API. It is generated from the OpenAPI snapshot committed in this repository
(`app/src/test/resources/openapi-snapshot.json`), so the client can never describe an API the backend does not serve: the backend's
`OpenApiSnapshotTest` fails the build when the live spec and the snapshot differ.

This is the complete API of the school backend: the platform operations it inherits (authentication, `/me`, users, documents, ...) and the school ones. The school frontend needs this package only.

| Entry point | Contents |
|---|---|
| `@school/api-client` | One `async` function and one TanStack Query hook per operation (`useMeMe`, `meMe`, ...), every request/response type, and the runtime below |
| `@school/api-client/zod` | Zod schemas for every operation's params, query, body and response, for forms and response validation |
| `@school/api-client/mocks` | MSW handlers and faker data per operation, for tests and Storybook |

## Install and pin

The client is released with the backend: tag `vX.Y.Z` attaches the tarball and `school-saas-openapi.json` to the GitHub release, and
publishes the package to the registry configured by the `NPM_REGISTRY_URL` repository variable.

```ini
# .npmrc of the frontend
@school:registry=<your registry>
```

```bash
pnpm add --save-exact @school/api-client@X.Y.Z
pnpm add @tanstack/react-query react zod      # peer dependencies
pnpm add -D msw @faker-js/faker               # only for @school/api-client/mocks
```

Pin an exact version. Upgrading is an ordinary pull request: a renamed, removed or newly required field fails the
frontend's type-check, which is exactly the review signal wanted.

## Use

```ts
import { configureApiClient } from '@school/api-client';

// Browser: call the BFF on the same origin. Server components: pass an absolute base URL and your own fetch.
configureApiClient({ baseUrl: '', headers: () => ({ 'X-Tenant-ID': tenantSlug }) });
```

```tsx
import { useMeMe } from '@school/api-client';

const { data, error } = useMeMe();            // data: ApiResponseMeResponse, error: ApiError<ApiErrorResponse>
```

`error` is an `ApiError`: `status`, `code` (the stable, translatable `error.code`), `traceId`, `requestId`, `fieldErrors`
and the raw `body`. Map `code` to a translated message in one place.

### Operations that require an `Idempotency-Key`

The spec lists the header on those operations, but the generator does not model header parameters, so pass the key
yourself and reuse it for every retry of the same user action. The server answers `400 IDEMPOTENCY_KEY_REQUIRED` when it is
missing.

```ts
import { withIdempotencyKey } from '@school/api-client';

const key = useMemo(() => crypto.randomUUID(), []);        // one per form
const invite = useUserBulkInvite({ request: withIdempotencyKey(key) });
invite.mutate({ data: { /* BulkInviteRequest */ } });
```

### Validation and mocks

```ts
import { AuthLoginBody } from '@school/api-client/zod';          // react-hook-form + zodResolver
import { getMeMeMockHandler } from '@school/api-client/mocks';   // server.use(getMeMeMockHandler())
```

## Develop

```bash
npm ci
npm run generate    # orval -> src/generated (git-ignored) + barrels
npm run typecheck
npm test
npm run build       # dist/, plain Node-ESM, no bundler needed
```

`OPENAPI_SPEC=/path/to/openapi.json npm run generate` generates from a spec that is not committed yet. Generated code is never
edited: change the backend (annotations, DTOs), regenerate the snapshot with `-DupdateOpenApiSnapshot=true`, and commit it.

CI runs generate, type-check, test and build on every pull request and prints the API changes (oasdiff) in the job summary.
