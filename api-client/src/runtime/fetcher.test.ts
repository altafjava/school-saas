import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch, configureApiClient, isApiError, withIdempotencyKey } from './index.js';

const json = (body: unknown, init: ResponseInit = {}): Response =>
  new Response(JSON.stringify(body), { headers: { 'Content-Type': 'application/json' }, ...init });

const lastCall = (fetchMock: ReturnType<typeof vi.fn>) => {
  const [url, init] = fetchMock.mock.calls.at(-1) as [string, RequestInit & { headers: Headers }];
  return { url, init, headers: init.headers };
};

describe('apiFetch', () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    fetchMock.mockReset();
    configureApiClient({ fetch: fetchMock });
  });

  it('prefixes the base url and asks for JSON first', async () => {
    configureApiClient({ fetch: fetchMock, baseUrl: 'https://bff.test' });
    fetchMock.mockResolvedValue(json({ ok: true }));

    await apiFetch('/api/v1/me', { method: 'GET' });

    const { url, headers } = lastCall(fetchMock);
    expect(url).toBe('https://bff.test/api/v1/me');
    expect(headers.get('Accept')).toBe('application/json, */*;q=0.8');
  });

  it('sends configured headers, resolving a function once per call, and lets call headers win', async () => {
    const tenant = vi.fn(() => ({ 'X-Tenant-ID': 'acme', 'X-Trace': 'default' }));
    configureApiClient({ fetch: fetchMock, headers: tenant });
    fetchMock.mockResolvedValue(json({}));

    await apiFetch('/x', { headers: { 'X-Trace': 'per-call' } });

    const { headers } = lastCall(fetchMock);
    expect(headers.get('X-Tenant-ID')).toBe('acme');
    expect(headers.get('X-Trace')).toBe('per-call');
    expect(tenant).toHaveBeenCalledTimes(1);
  });

  it('returns the parsed JSON body', async () => {
    fetchMock.mockResolvedValue(json({ success: true, data: { id: 7 } }));

    await expect(apiFetch('/x')).resolves.toEqual({ success: true, data: { id: 7 } });
  });

  it('returns undefined for 204 and for an empty JSON body', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    fetchMock.mockResolvedValueOnce(new Response('', { headers: { 'Content-Type': 'application/json' } }));

    await expect(apiFetch('/x')).resolves.toBeUndefined();
    await expect(apiFetch('/x')).resolves.toBeUndefined();
  });

  it('returns binary downloads as a Blob and text as a string', async () => {
    fetchMock.mockResolvedValueOnce(new Response('%PDF', { headers: { 'Content-Type': 'application/pdf' } }));
    fetchMock.mockResolvedValueOnce(new Response('a,b', { headers: { 'Content-Type': 'text/csv' } }));

    await expect(apiFetch<Blob>('/report')).resolves.toBeInstanceOf(Blob);
    await expect(apiFetch<string>('/export')).resolves.toBe('a,b');
  });

  it('throws an ApiError carrying the error envelope, trace id and request id', async () => {
    fetchMock.mockResolvedValue(
      json(
        {
          success: false,
          error: {
            code: 'VALIDATION_FAILED',
            message: 'Validation failed',
            traceId: 'trace-1',
            details: [{ field: 'email', message: 'must be a valid email' }],
          },
        },
        { status: 400, headers: { 'Content-Type': 'application/json', 'X-Request-Id': 'req-1' } },
      ),
    );

    const error = await apiFetch('/x').catch((e: unknown) => e);

    expect(isApiError(error)).toBe(true);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.code).toBe('VALIDATION_FAILED');
    expect(apiError.traceId).toBe('trace-1');
    expect(apiError.requestId).toBe('req-1');
    expect(apiError.message).toBe('Validation failed');
    expect(apiError.fieldErrors).toEqual([{ field: 'email', message: 'must be a valid email' }]);
  });

  it('keeps the status when the failure body is not JSON', async () => {
    fetchMock.mockResolvedValue(new Response('<html>Bad Gateway</html>', { status: 502 }));

    const error = (await apiFetch('/x').catch((e: unknown) => e)) as ApiError<string>;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(502);
    expect(error.body).toBe('<html>Bad Gateway</html>');
    expect(error.code).toBeUndefined();
    expect(error.fieldErrors).toEqual([]);
    expect(error.message).toBe('Request failed with status 502');
  });

  it('lets a network failure propagate untouched', async () => {
    fetchMock.mockRejectedValue(new TypeError('fetch failed'));

    await expect(apiFetch('/x')).rejects.toThrow('fetch failed');
  });
});

describe('withIdempotencyKey', () => {
  it('carries the given key, or a fresh UUID when none is given', () => {
    expect(withIdempotencyKey('k-1')).toEqual({ headers: { 'Idempotency-Key': 'k-1' } });

    const generated = withIdempotencyKey().headers as Record<string, string>;
    expect(generated['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/);
  });
});
