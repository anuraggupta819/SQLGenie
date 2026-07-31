import { apiClient } from './client';
import type { PageResponse, QueryHistoryEntry, QueryResultResponse } from '../types';

export function submitQuery(question: string) {
  return apiClient
    .post<QueryResultResponse>('/api/v1/queries', { question })
    .then((res) => res.data);
}

export function getHistory(page = 0, size = 20) {
  return apiClient
    .get<PageResponse<QueryHistoryEntry>>('/api/v1/queries/history', { params: { page, size } })
    .then((res) => res.data);
}

export function deleteHistoryEntry(id: string) {
  return apiClient.delete(`/api/v1/queries/history/${id}`);
}
