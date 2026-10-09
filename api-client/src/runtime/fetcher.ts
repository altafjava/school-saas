import { ApiError } from './api-error.js';

export interface ApiClientConfig {
  /** Prepended to every request path. Empty for browser calls to the same origin (the BFF); absolute on the server. */
  baseUrl?: string;
  /** Sent with every request, e.g. `X-Tenant-ID`. Headers passed per call win. */
  headers?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>);
  /** Replaces the global `fetch`, e.g. to forward cookies from a server component. */
  fetch?: typeof globalThis.fetch;
}

// JSON where the endpoint offers it, anything else (PDF, CSV downloads) rather than a 406.
const ACCEPT_JSON_FIRST = 'application/json, */*;q=0.8';

let config: ApiClientConfig = {};

export const configureApiClient = (next: ApiClientConfig): void => {
  config = { ...next };
};

/** Orval mutator: every generated function and hook sends its request through here. */
export const apiFetch = async <T>(url: string, init: RequestInit = {}): Promise<T> => {
  const send = config.fetch ?? globalThis.fetch;
  const headers = await mergeHeaders(init.headers);
  const response = await send(`${config.baseUrl ?? ''}${url}`, { ...init, headers });
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorBody(response), response.headers.get('X-Request-Id'));
  }
  return (await readBody(response)) as T;
};

/** Typed error for the generated hooks' `error` field. */
export type ErrorType<TBody> = ApiError<TBody>;

const mergeHeaders = async (callHeaders: HeadersInit | undefined): Promise<Headers> => {
  const configured = typeof config.headers === 'function' ? await config.headers() : config.headers;
  const merged = new Headers(configured);
  new Headers(callHeaders).forEach((value, name) => merged.set(name, value));
  if (!merged.has('Accept')) {
    merged.set('Accept', ACCEPT_JSON_FIRST);
  }
  return merged;
};

const readBody = async (response: Response): Promise<unknown> => {
  if (response.status === 204 || response.status === 205) {
    return undefined;
  }
  const contentType = response.headers.get('Content-Type') ?? '';
  if (contentType.includes('json')) {
    const text = await response.text();
    return text === '' ? undefined : (JSON.parse(text) as unknown);
  }
  return contentType.startsWith('text/') ? response.text() : response.blob();
};

/** A failure body that is not valid JSON (e.g. a gateway's HTML page) must not hide the status code. */
const readErrorBody = async (response: Response): Promise<unknown> => {
  const text = await response.text();
  if (text === '') {
    return undefined;
  }
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
};

/** The generator drops header parameters: pass `request: withIdempotencyKey(key)` and reuse the key for every retry of one user action. */
export const withIdempotencyKey = (key: string = crypto.randomUUID()): Pick<RequestInit, 'headers'> => ({
  headers: { 'Idempotency-Key': key },
});
