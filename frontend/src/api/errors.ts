import { isAxiosError } from 'axios';
import type { ApiErrorResponse } from '../types';

export function getErrorMessage(error: unknown): string {
  if (isAxiosError<ApiErrorResponse>(error)) {
    const details = error.response?.data?.details;
    if (details && details.length > 0) {
      return details.join(', ');
    }
    return error.response?.data?.message ?? error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong';
}
