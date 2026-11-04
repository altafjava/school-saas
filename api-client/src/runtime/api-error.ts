/** One rejected field. Translate by `code` and `params`; `message` is the English fallback. */
export interface FieldError {
  field?: string;
  /** The failed rule, e.g. `NOT_BLANK`, `SIZE`, `PATTERN`, `INVALID_FORMAT`. */
  code?: string;
  message?: string;
  /** The rule's limits, e.g. `{ min: 3, max: 50 }` for `SIZE`; absent when it has none. */
  params?: Record<string, unknown>;
}

/** Shape of the failure envelope every endpoint returns (`ApiErrorResponse` in the generated models). */
export interface ApiErrorBody {
  success?: boolean;
  error?: {
    code?: string;
    message?: string;
    traceId?: string;
    details?: FieldError[];
    /** Values for translating `code`, e.g. `{ maxDays: 31 }`; absent when the code has none. */
    params?: Record<string, unknown>;
  };
}

/** A non-2xx response. `code` is the stable, translatable `error.code`; quote `traceId` when reporting a problem. */
export class ApiError<TBody = unknown> extends Error {
  readonly status: number;
  readonly body: TBody | undefined;
  readonly requestId: string | null;

  constructor(status: number, body: TBody | undefined, requestId: string | null) {
    super(describe(status, body));
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
    this.requestId = requestId;
  }

  get code(): string | undefined {
    return asErrorBody(this.body)?.error?.code;
  }

  get traceId(): string | undefined {
    return asErrorBody(this.body)?.error?.traceId;
  }

  /** Values to interpolate into the translation of `code`, empty when the code has none. */
  get params(): Record<string, unknown> {
    return asErrorBody(this.body)?.error?.params ?? {};
  }

  /** Field-level validation failures, empty when the error is not a validation error. */
  get fieldErrors(): FieldError[] {
    return asErrorBody(this.body)?.error?.details ?? [];
  }
}

export const isApiError = (value: unknown): value is ApiError => value instanceof ApiError;

const asErrorBody = (body: unknown): ApiErrorBody | undefined =>
  typeof body === 'object' && body !== null ? (body as ApiErrorBody) : undefined;

const describe = (status: number, body: unknown): string =>
  asErrorBody(body)?.error?.message ?? `Request failed with status ${status}`;
