-- V2__seed_initial_users.sql
-- Demonstration Accounts Seed Data (Password: Password123!)
-- Verified with Spring Security BCryptPasswordEncoder (10 rounds):
-- Hash: $2a$10$qPEhI62xUoh8O6hfww/0Pe6WGMGDgp38U9cGd/heuNIOP94ueFiWe

INSERT INTO users (username, email, password_hash, full_name, role, institution, active)
VALUES 
('admin', 'admin@aquascope.org', '$2a$10$qPEhI62xUoh8O6hfww/0Pe6WGMGDgp38U9cGd/heuNIOP94ueFiWe', 'Dr. Elizabeth Vance (Demo Admin)', 'ADMINISTRATOR', 'Global Water Environmental Council', true),
('researcher', 'researcher@aquascope.org', '$2a$10$qPEhI62xUoh8O6hfww/0Pe6WGMGDgp38U9cGd/heuNIOP94ueFiWe', 'Elena Rostova (Demo Researcher)', 'LABORATORY_RESEARCHER', 'Microplastic Spectroscopy Lab', true),
('field_officer', 'field@aquascope.org', '$2a$10$qPEhI62xUoh8O6hfww/0Pe6WGMGDgp38U9cGd/heuNIOP94ueFiWe', 'Marcus Chen (Demo Field Officer)', 'ENVIRONMENTAL_FIELD_OFFICER', 'River Basin Monitoring Unit', true),
('reviewer', 'reviewer@aquascope.org', '$2a$10$qPEhI62xUoh8O6hfww/0Pe6WGMGDgp38U9cGd/heuNIOP94ueFiWe', 'Prof. Arthur Pendelton (Demo Reviewer)', 'REVIEWER', 'Environmental Quality Assurance Board', true)
ON CONFLICT (username) DO NOTHING;

-- Initial reference monitoring site (Demonstration Sites)
INSERT INTO monitoring_sites (code, name, description, water_source_category, latitude, longitude, elevation_meters, active)
VALUES
('SITE-HUD-01', 'Hudson Estuary Station Alpha (Demo)', 'Lower estuary monitoring station recording tidal and freshwater boundary', 'ESTUARY', 40.75870000, -74.00480000, 2.50, true),
('SITE-ERIE-04', 'Lake Erie Shoreline Beta (Demo)', 'Nearshore shallow sampling point vulnerable to urban plastic runoff', 'LAKE', 41.50550000, -81.69340000, 174.00, true)
ON CONFLICT (code) DO NOTHING;
