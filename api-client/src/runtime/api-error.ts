/** Shape of the failure envelope every endpoint returns (`ApiErrorResponse` in the generated models). */
export interface ApiErrorBody {
  success?: boolean;
  error?: {
    code?: string;
    message?: string;
    traceId?: string;
    details?: { field?: string; message?: string }[];
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

  /** Field-level validation failures, empty when the error is not a validation error. */
  get fieldErrors(): { field?: string; message?: string }[] {
    return asErrorBody(this.body)?.error?.details ?? [];
  }
}

export const isApiError = (value: unknown): value is ApiError => value instanceof ApiError;

const asErrorBody = (body: unknown): ApiErrorBody | undefined =>
  typeof body === 'object' && body !== null ? (body as ApiErrorBody) : undefined;

const describe = (status: number, body: unknown): string =>
  asErrorBody(body)?.error?.message ?? `Request failed with status ${status}`;
