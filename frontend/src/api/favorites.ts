import { apiClient } from './client';
import type { FavoriteQuery } from '../types';

export function getFavorites() {
  return apiClient.get<FavoriteQuery[]>('/api/v1/favorites').then((res) => res.data);
}

export function saveFavorite(name: string, naturalLanguageQuery: string, generatedSql: string) {
  return apiClient
    .post<FavoriteQuery>('/api/v1/favorites', { name, naturalLanguageQuery, generatedSql })
    .then((res) => res.data);
}

export function deleteFavorite(id: string) {
  return apiClient.delete(`/api/v1/favorites/${id}`);
}
