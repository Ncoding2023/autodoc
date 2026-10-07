import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

export type ApiErrorResponse = {
  code: string;
  message: string;
};

export const getApiError = (error: unknown): ApiErrorResponse | null => {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return null;
  }

  return error.response?.data ?? null;
};
