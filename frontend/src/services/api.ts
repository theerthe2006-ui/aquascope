import axios from 'axios';
import type { 
  ApiResponse, 
  HealthResponse, 
  AuthResponse, 
  UserDto, 
  MonitoringSiteDto, 
  WaterSampleDto, 
  SampleAnalysisDto, 
  ParticleObservationDto,
  AnalysisReviewDto, 
  SampleImageDto,
  DeviceDto,
  SensorReadingDto,
  AuditLogDto,
  DashboardStatsDto 
} from '../types/api';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Intercept requests to attach JWT Bearer token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('aquascope_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Intercept 401 unauthorized to automatically clear session
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('aquascope_token');
      localStorage.removeItem('aquascope_user');
    }
    return Promise.reject(error);
  }
);

export const healthApi = {
  getHealth: async (): Promise<ApiResponse<HealthResponse>> => {
    const response = await api.get<ApiResponse<HealthResponse>>('/health');
    return response.data;
  },
};

export const authApi = {
  login: async (username: string, password: string): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', { username, password });
    return response.data;
  },
  getMe: async (): Promise<ApiResponse<UserDto>> => {
    const response = await api.get<ApiResponse<UserDto>>('/auth/me');
    return response.data;
  },
  getAllUsers: async (): Promise<ApiResponse<UserDto[]>> => {
    const response = await api.get<ApiResponse<UserDto[]>>('/auth/users');
    return response.data;
  },
  registerUser: async (user: any): Promise<ApiResponse<UserDto>> => {
    const response = await api.post<ApiResponse<UserDto>>('/auth/users', user);
    return response.data;
  },
  toggleUserStatus: async (id: number): Promise<ApiResponse<UserDto>> => {
    const response = await api.patch<ApiResponse<UserDto>>(`/auth/users/${id}/status`);
    return response.data;
  },
};

export const dashboardApi = {
  getStats: async (): Promise<ApiResponse<DashboardStatsDto>> => {
    const response = await api.get<ApiResponse<DashboardStatsDto>>('/dashboard/stats');
    return response.data;
  },
};

export const siteApi = {
  getAll: async (): Promise<ApiResponse<MonitoringSiteDto[]>> => {
    const response = await api.get<ApiResponse<MonitoringSiteDto[]>>('/monitoring-sites');
    return response.data;
  },
  create: async (site: MonitoringSiteDto): Promise<ApiResponse<MonitoringSiteDto>> => {
    const response = await api.post<ApiResponse<MonitoringSiteDto>>('/monitoring-sites', site);
    return response.data;
  },
  update: async (id: number, site: MonitoringSiteDto): Promise<ApiResponse<MonitoringSiteDto>> => {
    const response = await api.put<ApiResponse<MonitoringSiteDto>>(`/monitoring-sites/${id}`, site);
    return response.data;
  },
  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await api.delete<ApiResponse<void>>(`/monitoring-sites/${id}`);
    return response.data;
  },
};

export const sampleApi = {
  getAll: async (): Promise<ApiResponse<WaterSampleDto[]>> => {
    const response = await api.get<ApiResponse<WaterSampleDto[]>>('/water-samples');
    return response.data;
  },
  create: async (sample: Partial<WaterSampleDto>): Promise<ApiResponse<WaterSampleDto>> => {
    const response = await api.post<ApiResponse<WaterSampleDto>>('/water-samples', sample);
    return response.data;
  },
  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await api.delete<ApiResponse<void>>(`/water-samples/${id}`);
    return response.data;
  },
};

export const analysisApi = {
  getAll: async (): Promise<ApiResponse<SampleAnalysisDto[]>> => {
    const response = await api.get<ApiResponse<SampleAnalysisDto[]>>('/analyses');
    return response.data;
  },
  getById: async (id: number): Promise<ApiResponse<SampleAnalysisDto>> => {
    const response = await api.get<ApiResponse<SampleAnalysisDto>>(`/analyses/${id}`);
    return response.data;
  },
  create: async (analysis: Partial<SampleAnalysisDto>): Promise<ApiResponse<SampleAnalysisDto>> => {
    const response = await api.post<ApiResponse<SampleAnalysisDto>>('/analyses', analysis);
    return response.data;
  },
  addObservation: async (analysisId: number, obs: Partial<ParticleObservationDto>): Promise<ApiResponse<ParticleObservationDto>> => {
    const response = await api.post<ApiResponse<ParticleObservationDto>>(`/analyses/${analysisId}/observations`, obs);
    return response.data;
  },
  submitForReview: async (id: number): Promise<ApiResponse<SampleAnalysisDto>> => {
    const response = await api.post<ApiResponse<SampleAnalysisDto>>(`/analyses/${id}/submit`);
    return response.data;
  },
};

export const reviewApi = {
  submitReview: async (analysisId: number, review: Partial<AnalysisReviewDto>): Promise<ApiResponse<AnalysisReviewDto>> => {
    const response = await api.post<ApiResponse<AnalysisReviewDto>>(`/reviews/analysis/${analysisId}`, review);
    return response.data;
  },
};

export const deviceApi = {
  getAll: async (): Promise<ApiResponse<DeviceDto[]>> => {
    const response = await api.get<ApiResponse<DeviceDto[]>>('/devices');
    return response.data;
  },
  create: async (device: Partial<DeviceDto>): Promise<ApiResponse<DeviceDto>> => {
    const response = await api.post<ApiResponse<DeviceDto>>('/devices', device);
    return response.data;
  },
  update: async (id: number, device: Partial<DeviceDto>): Promise<ApiResponse<DeviceDto>> => {
    const response = await api.put<ApiResponse<DeviceDto>>(`/devices/${id}`, device);
    return response.data;
  },
  getReadings: async (deviceId: number): Promise<ApiResponse<SensorReadingDto[]>> => {
    const response = await api.get<ApiResponse<SensorReadingDto[]>>(`/devices/${deviceId}/readings`);
    return response.data;
  },
  recordReading: async (reading: Partial<SensorReadingDto>): Promise<ApiResponse<SensorReadingDto>> => {
    const response = await api.post<ApiResponse<SensorReadingDto>>('/devices/readings', reading);
    return response.data;
  },
};

export const evidenceApi = {
  getByAnalysis: async (analysisId: number): Promise<ApiResponse<SampleImageDto[]>> => {
    const response = await api.get<ApiResponse<SampleImageDto[]>>(`/evidence/analysis/${analysisId}`);
    return response.data;
  },
  getBySample: async (sampleId: number): Promise<ApiResponse<SampleImageDto[]>> => {
    const response = await api.get<ApiResponse<SampleImageDto[]>>(`/evidence/sample/${sampleId}`);
    return response.data;
  },
  upload: async (formData: FormData): Promise<ApiResponse<SampleImageDto>> => {
    const response = await api.post<ApiResponse<SampleImageDto>>('/evidence', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  getDownloadUrl: (id: number): string => {
    const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
    return `${base}/evidence/${id}/download`;
  },
};

export const auditApi = {
  getRecent: async (limit: number = 20): Promise<ApiResponse<AuditLogDto[]>> => {
    const response = await api.get<ApiResponse<AuditLogDto[]>>(`/audit/recent?limit=${limit}`);
    return response.data;
  },
};

export const reportApi = {
  downloadCsv: async (): Promise<Blob> => {
    const response = await api.get('/reports/csv', { responseType: 'blob' });
    return response.data;
  },
  downloadPdf: async (): Promise<Blob> => {
    const response = await api.get('/reports/pdf', { responseType: 'blob' });
    return response.data;
  },
};

export default api;
