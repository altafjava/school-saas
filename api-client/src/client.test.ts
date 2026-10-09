import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { ApiError, configureApiClient, meMe, useMeMe } from './index.js';
import type { ApiResponseMeResponse } from './index.js';
import { MeMeResponse } from './zod.js';

const server = setupServer();

const session = {
  success: true,
  data: { user: { id: 'u-1', email: 'admin@acme.test', emailVerified: true, twoFactorEnabled: false } },
};

describe('generated client', () => {
  beforeAll(() => server.listen({ onUnhandledFrame: 'error' }));
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());
  beforeEach(() => configureApiClient({ baseUrl: 'http://api.test' }));

  it('sends a generated call through the runtime and returns the typed envelope', async () => {
    server.use(http.get('http://api.test/api/v1/me', () => HttpResponse.json(session)));

    const response: ApiResponseMeResponse = await meMe();

    expect(response.success).toBe(true);
    expect(response.data?.user?.email).toBe('admin@acme.test');
  });

  it('rejects with a typed ApiError carrying the documented error code', async () => {
    server.use(
      http.get('http://api.test/api/v1/me', () =>
        HttpResponse.json(
          { success: false, error: { code: 'TOKEN_EXPIRED', message: 'Expired', traceId: 't-9' } },
          { status: 401 },
        ),
      ),
    );

    const error = (await meMe().catch((e: unknown) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(401);
    expect(error.code).toBe('TOKEN_EXPIRED');
  });

  it('ships a response validator that accepts the contract shape and rejects a wrong one', () => {
    expect(MeMeResponse.safeParse(session).success).toBe(true);
    expect(MeMeResponse.safeParse({ success: 'yes' }).success).toBe(false);
  });

  it('exposes the query hook for the same operation', () => {
    expect(typeof useMeMe).toBe('function');
  });
});
