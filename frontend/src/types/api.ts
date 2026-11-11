export type Role = 'ADMINISTRATOR' | 'LABORATORY_RESEARCHER' | 'ENVIRONMENTAL_FIELD_OFFICER' | 'REVIEWER';

export type WaterSourceCategory = 'RIVER' | 'LAKE' | 'ESTUARY' | 'COASTAL' | 'GROUNDWATER' | 'WASTEWATER' | 'TAP_WATER' | 'RESERVOIR';

export type DataOrigin = 'SIMULATED' | 'MANUAL' | 'DEVICE';

export type SampleStatus = 'REGISTERED' | 'IN_ANALYSIS' | 'ANALYZED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';

export type AnalysisStatus = 'DRAFT' | 'COMPLETED' | 'SUBMITTED_FOR_REVIEW' | 'APPROVED' | 'REJECTED';

export type ParticleShape = 'FIBER' | 'FRAGMENT' | 'FILM' | 'BEAD_PELLET' | 'FOAM' | 'OTHER';

export type PolymerType = 'PE' | 'PP' | 'PS' | 'PVC' | 'PET' | 'PA_NYLON' | 'PU' | 'ACRYLIC' | 'OTHER' | 'UNVERIFIED';

export type IdentificationMethod = 'MORPHOLOGICAL_ONLY' | 'FTIR_MATCH' | 'RAMAN_MATCH' | 'PY_GC_MS' | 'HOT_NEEDLE_TEST';

export type DeviceStatus = 'ACTIVE' | 'INACTIVE' | 'CALIBRATION_REQUIRED' | 'OFFLINE';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  errors?: Record<string, string>;
}

export interface HealthResponse {
  status: string;
  timestamp: string;
  version: string;
  database: string;
  environment: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  institution: string;
}

export interface UserDto {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  institution: string;
  active: boolean;
  createdAt: string;
}

export interface MonitoringSiteDto {
  id?: number;
  code: string;
  name: string;
  description?: string;
  waterSourceCategory: WaterSourceCategory;
  latitude?: number;
  longitude?: number;
  elevationMeters?: number;
  createdByUsername?: string;
  active: boolean;
  createdAt?: string;
}

export interface ParticleObservationDto {
  id?: number;
  particleCount: number;
  sizeMicrometers: number;
  lengthMicrometers?: number;
  widthMicrometers?: number;
  shape: ParticleShape;
  color?: string;
  polymerType: PolymerType;
  identificationMethod: IdentificationMethod;
  confidenceLevel?: string;
  spectralMatchScore?: number;
  notes?: string;
}

export interface WaterSampleDto {
  id?: number;
  sampleIdentifier: string;
  monitoringSiteId: number;
  monitoringSiteCode?: string;
  monitoringSiteName?: string;
  collectedByUsername?: string;
  collectionDate: string;
  sampleVolume: number;
  volumeUnit: string;
  samplingDepthMeters?: number;
  waterTemperatureCelsius?: number;
  phLevel?: number;
  turbidityNtu?: number;
  collectionSource: string;
  weatherConditions?: string;
  dataOrigin: DataOrigin;
  status: SampleStatus;
  notes?: string;
  createdAt?: string;
}

export interface SampleAnalysisDto {
  id?: number;
  waterSampleId: number;
  sampleIdentifier?: string;
  analystUsername?: string;
  analysisDate: string;
  analysisMethod: string;
  equipmentModel?: string;
  magnification?: string;
  preparationMethod?: string;
  filterMaterial?: string;
  filterPoreSizeUm?: number;
  totalParticlesCounted: number;
  concentrationParticlesPerLiter?: number;
  status: AnalysisStatus;
  dataOrigin: DataOrigin;
  notes?: string;
  observations: ParticleObservationDto[];
  createdAt?: string;
}

export interface AnalysisReviewDto {
  id?: number;
  sampleAnalysisId?: number;
  reviewerUsername?: string;
  decision: string;
  comments: string;
  reviewDate?: string;
}

export interface SampleImageDto {
  id?: number;
  waterSampleId?: number;
  sampleAnalysisId?: number;
  uploadedByUserId?: number;
  uploadedByUsername?: string;
  fileName: string;
  storedFilePath: string;
  contentType: string;
  fileSizeBytes: number;
  imagingModality: string;
  magnification?: string;
  description?: string;
  createdAt?: string;
}

export interface DeviceDto {
  id?: number;
  deviceIdentifier: string;
  name: string;
  deviceType: string;
  monitoringSiteId?: number;
  monitoringSiteCode?: string;
  manufacturer?: string;
  firmwareVersion?: string;
  status: DeviceStatus;
  lastPing?: string;
  createdAt?: string;
}

export interface SensorReadingDto {
  id?: number;
  deviceId: number;
  deviceIdentifier?: string;
  readingTimestamp: string;
  metricType: string;
  metricValue: number;
  unit: string;
  dataOrigin: DataOrigin;
  rawPayload?: string;
  createdAt?: string;
}

export interface AuditLogDto {
  id: number;
  userId?: number;
  username: string;
  action: string;
  entityType: string;
  entityId?: string;
  dataOrigin: DataOrigin;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface DashboardStatsDto {
  totalMonitoringSites: number;
  totalWaterSamples: number;
  pendingAnalyses: number;
  completedAnalyses: number;
  pendingReviews: number;
  approvedAnalyses: number;
  rejectedAnalyses: number;
  totalParticleObservations: number;
  dataOriginDistribution: Record<DataOrigin, number>;
  particleShapeDistribution: Record<ParticleShape, number>;
  polymerTypeDistribution: Record<PolymerType, number>;
}
