import { apiClient } from './client';
import type { AuthResponse } from '../types';

export function register(email: string, password: string, fullName: string) {
  return apiClient
    .post<AuthResponse>('/api/v1/auth/register', { email, password, fullName })
    .then((res) => res.data);
}

export function login(email: string, password: string) {
  return apiClient
    .post<AuthResponse>('/api/v1/auth/login', { email, password })
    .then((res) => res.data);
}

export function logout(refreshToken: string) {
  return apiClient.post('/api/v1/auth/logout', { refreshToken });
}
