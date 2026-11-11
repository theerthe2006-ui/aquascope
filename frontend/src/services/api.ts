import axios from 'axios';
import type { ApiResponse, HealthResponse } from '../types/api';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: 10000,
});

export const healthApi = {
  getHealth: async (): Promise<ApiResponse<HealthResponse>> => {
    const response = await api.get<ApiResponse<HealthResponse>>('/health');
    return response.data;
  },
};

export default api;
