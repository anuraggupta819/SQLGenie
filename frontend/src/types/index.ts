export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export type QueryStatus = 'SUCCESS' | 'REJECTED' | 'FAILED';

export interface QueryResultResponse {
  status: QueryStatus;
  sql: string | null;
  explanation: string | null;
  columns: string[] | null;
  rows: Record<string, unknown>[] | null;
  executionTimeMs: number | null;
  errorMessage: string | null;
}

export interface QueryHistoryEntry {
  id: string;
  naturalLanguageQuery: string;
  generatedSql: string | null;
  explanation: string | null;
  status: QueryStatus;
  errorMessage: string | null;
  executionTimeMs: number | null;
  rowCount: number | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FavoriteQuery {
  id: string;
  name: string;
  naturalLanguageQuery: string;
  generatedSql: string;
  explanation: string;
  createdAt: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details: string[];
}
