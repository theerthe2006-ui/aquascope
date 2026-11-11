export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  errors?: Record<string, string>;
}

export interface DatabaseHealth {
  connected: boolean;
  databaseType: string;
  details: string;
}

export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  application: string;
  version: string;
  environment: string;
  timestamp: string;
  database: DatabaseHealth;
}
