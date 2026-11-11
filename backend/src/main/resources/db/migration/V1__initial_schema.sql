-- V1__initial_schema.sql
-- AQUASCOPE: Full Relational Domain Schema with Scientific Integrity & Traceability

-- 1. Users & Roles
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL, -- ADMINISTRATOR, LABORATORY_RESEARCHER, ENVIRONMENTAL_FIELD_OFFICER, REVIEWER
    institution VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Monitoring Sites (Water bodies / stations)
CREATE TABLE IF NOT EXISTS monitoring_sites (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    water_source_category VARCHAR(100) NOT NULL, -- RIVER, LAKE, COASTAL, ESTUARY, RESERVOIR, TAP_WATER, WASTEWATER
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    elevation_meters DECIMAL(8, 2),
    created_by_user_id BIGINT REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Water Samples
CREATE TABLE IF NOT EXISTS water_samples (
    id BIGSERIAL PRIMARY KEY,
    sample_identifier VARCHAR(100) NOT NULL UNIQUE, -- e.g. AQ-2026-RIV-001
    monitoring_site_id BIGINT NOT NULL REFERENCES monitoring_sites(id),
    collected_by_user_id BIGINT REFERENCES users(id),
    collection_date TIMESTAMP WITH TIME ZONE NOT NULL,
    sample_volume DECIMAL(12, 4) NOT NULL,
    volume_unit VARCHAR(20) NOT NULL DEFAULT 'L', -- L, mL, m3
    sampling_depth_meters DECIMAL(6, 2),
    water_temperature_celsius DECIMAL(5, 2),
    ph_level DECIMAL(4, 2),
    turbidity_ntu DECIMAL(7, 2),
    collection_source VARCHAR(100) NOT NULL, -- SURFACE, MID_COLUMN, SEDIMENT_INTERACTION, EFFLUENT
    weather_conditions VARCHAR(150),
    data_origin VARCHAR(50) NOT NULL DEFAULT 'MANUAL', -- MANUAL, DEVICE, SIMULATED
    status VARCHAR(50) NOT NULL DEFAULT 'REGISTERED', -- REGISTERED, IN_ANALYSIS, ANALYZED, UNDER_REVIEW, APPROVED, REJECTED
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Sample Analyses (Laboratory records)
CREATE TABLE IF NOT EXISTS sample_analyses (
    id BIGSERIAL PRIMARY KEY,
    water_sample_id BIGINT NOT NULL REFERENCES water_samples(id),
    analyst_user_id BIGINT NOT NULL REFERENCES users(id),
    analysis_date TIMESTAMP WITH TIME ZONE NOT NULL,
    analysis_method VARCHAR(100) NOT NULL, -- OPTICAL_MICROSCOPY, FTIR_SPECTROSCOPY, RAMAN_SPECTROSCOPY, PY_GC_MS, FLUORESCENCE_MICROSCOPY
    equipment_model VARCHAR(200),
    magnification VARCHAR(50),
    preparation_method VARCHAR(200), -- WET_PEROXIDE_DIGESTION, DENSITY_SEPARATION, ENZYMATIC_DIGESTION, DIRECT_FILTRATION
    filter_material VARCHAR(100), -- GLASS_FIBER, POLYCARBONATE, CELLULOSE_NITRATE, ANODISC
    filter_pore_size_um DECIMAL(6, 2),
    total_particles_counted INTEGER NOT NULL DEFAULT 0,
    concentration_particles_per_liter DECIMAL(12, 4),
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED', -- DRAFT, COMPLETED, SUBMITTED_FOR_REVIEW, APPROVED, REJECTED
    data_origin VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Particle Observations (Individual particle breakdown)
CREATE TABLE IF NOT EXISTS particle_observations (
    id BIGSERIAL PRIMARY KEY,
    sample_analysis_id BIGINT NOT NULL REFERENCES sample_analyses(id) ON DELETE CASCADE,
    particle_count INTEGER NOT NULL DEFAULT 1,
    size_micrometers DECIMAL(10, 2) NOT NULL,
    length_micrometers DECIMAL(10, 2),
    width_micrometers DECIMAL(10, 2),
    shape VARCHAR(50) NOT NULL, -- FIBER, FRAGMENT, PELLET, FILM, FOAM, BEAD, SPHERE
    color VARCHAR(50), -- TRANSPARENT, WHITE, BLACK, BLUE, RED, GREEN, YELLOW, OTHER
    polymer_type VARCHAR(100) NOT NULL DEFAULT 'UNVERIFIED', -- PE, PP, PS, PVC, PET, PA_NYLON, PU, OTHER, UNVERIFIED
    identification_method VARCHAR(100) NOT NULL, -- MORPHOLOGICAL_ONLY, FTIR_MATCH, RAMAN_MATCH, HOT_NEEDLE_TEST
    confidence_level VARCHAR(50) NOT NULL DEFAULT 'LOW', -- LOW, MEDIUM, HIGH, DEFINITIVE
    spectral_match_score DECIMAL(5, 2), -- e.g. 85.5% if FTIR library match
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Sample Images & Microscopy Evidence
CREATE TABLE IF NOT EXISTS sample_images (
    id BIGSERIAL PRIMARY KEY,
    water_sample_id BIGINT REFERENCES water_samples(id),
    sample_analysis_id BIGINT REFERENCES sample_analyses(id),
    uploaded_by_user_id BIGINT NOT NULL REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    imaging_modality VARCHAR(100) NOT NULL DEFAULT 'BRIGHTFIELD_MICROSCOPY', -- BRIGHTFIELD, FLUORESCENCE, STEREOMICROSCOPE, SEM
    magnification VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Review Records (Traceable approval decisions)
CREATE TABLE IF NOT EXISTS analysis_reviews (
    id BIGSERIAL PRIMARY KEY,
    sample_analysis_id BIGINT NOT NULL REFERENCES sample_analyses(id),
    reviewer_user_id BIGINT NOT NULL REFERENCES users(id),
    decision VARCHAR(50) NOT NULL, -- APPROVED, REJECTED, CORRECTION_REQUESTED
    comments TEXT NOT NULL,
    review_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Future Hardware / IoT Devices Registry
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    device_identifier VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    device_type VARCHAR(100) NOT NULL, -- OPTICAL_IN_SITU_SENSOR, AUTOSAMPLER, MULTIPARAMETER_SONDE
    monitoring_site_id BIGINT REFERENCES monitoring_sites(id),
    manufacturer VARCHAR(150),
    firmware_version VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, CALIBRATION_REQUIRED, OFFLINE
    last_ping TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Device Sensor Readings (API contracts for future telemetry)
CREATE TABLE IF NOT EXISTS sensor_readings (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id),
    reading_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    metric_type VARCHAR(100) NOT NULL, -- TURBIDITY, FLUORESCENCE_ESTIMATE, FLOW_RATE, TEMPERATURE, CONDUCTIVITY
    metric_value DECIMAL(12, 4) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    data_origin VARCHAR(50) NOT NULL DEFAULT 'DEVICE', -- DEVICE, SIMULATED
    raw_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. Audit Logs (Immutable action log)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    username VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    data_origin VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for high-performance querying and reporting
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_monitoring_sites_code ON monitoring_sites(code);
CREATE INDEX IF NOT EXISTS idx_water_samples_identifier ON water_samples(sample_identifier);
CREATE INDEX IF NOT EXISTS idx_water_samples_site ON water_samples(monitoring_site_id);
CREATE INDEX IF NOT EXISTS idx_water_samples_status ON water_samples(status);
CREATE INDEX IF NOT EXISTS idx_water_samples_collection_date ON water_samples(collection_date);
CREATE INDEX IF NOT EXISTS idx_sample_analyses_sample ON sample_analyses(water_sample_id);
CREATE INDEX IF NOT EXISTS idx_sample_analyses_status ON sample_analyses(status);
CREATE INDEX IF NOT EXISTS idx_particle_observations_analysis ON particle_observations(sample_analysis_id);
CREATE INDEX IF NOT EXISTS idx_particle_observations_shape ON particle_observations(shape);
CREATE INDEX IF NOT EXISTS idx_particle_observations_polymer ON particle_observations(polymer_type);
CREATE INDEX IF NOT EXISTS idx_sample_images_sample ON sample_images(water_sample_id);
CREATE INDEX IF NOT EXISTS idx_sample_images_analysis ON sample_images(sample_analysis_id);
CREATE INDEX IF NOT EXISTS idx_devices_identifier ON devices(device_identifier);
CREATE INDEX IF NOT EXISTS idx_sensor_readings_device ON sensor_readings(device_id);
CREATE INDEX IF NOT EXISTS idx_sensor_readings_timestamp ON sensor_readings(reading_timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
