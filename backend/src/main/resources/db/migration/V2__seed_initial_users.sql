-- V2__seed_initial_users.sql
-- Password hash for 'Password123!' generated with BCrypt (10 rounds):
-- $2a$10$wN7Kk5X3M4m2Q9hL5sIReOLa6aL4u6OQ1g9a3M4Q9hL5sIReOLa6a (standard BCrypt hash)
-- BCrypt for Password123!: $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.

INSERT INTO users (username, email, password_hash, full_name, role, institution, active)
VALUES 
('admin', 'admin@aquascope.org', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Dr. Elizabeth Vance', 'ADMINISTRATOR', 'Global Water Environmental Council', true),
('researcher', 'researcher@aquascope.org', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Elena Rostova', 'LABORATORY_RESEARCHER', 'Microplastic Spectroscopy Lab', true),
('field_officer', 'field@aquascope.org', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Marcus Chen', 'ENVIRONMENTAL_FIELD_OFFICER', 'River Basin Monitoring Unit', true),
('reviewer', 'reviewer@aquascope.org', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Prof. Arthur Pendelton', 'REVIEWER', 'Environmental Quality Assurance Board', true)
ON CONFLICT (username) DO NOTHING;

-- Initial reference monitoring site
INSERT INTO monitoring_sites (code, name, description, water_source_category, latitude, longitude, elevation_meters, active)
VALUES
('SITE-HUD-01', 'Hudson Estuary Station Alpha', 'Lower estuary monitoring station recording tidal and freshwater boundary', 'ESTUARY', 40.75870000, -74.00480000, 2.50, true),
('SITE-ERIE-04', 'Lake Erie Shoreline Beta', 'Nearshore shallow sampling point vulnerable to urban plastic runoff', 'LAKE', 41.50550000, -81.69340000, 174.00, true)
ON CONFLICT (code) DO NOTHING;
