-- Database Setup for AQUASCOPE
-- Run this as superuser 'postgres' in psql:
-- psql -U postgres -f setup_database.sql

-- 1. Create dedicated application user
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles WHERE rolname = 'aquascope_user') THEN
      CREATE ROLE aquascope_user LOGIN PASSWORD 'AquaScopeSecure2026!';
   END IF;
END
$do$;

-- 2. Create database owned by aquascope_user
SELECT 'CREATE DATABASE aquascope OWNER aquascope_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'aquascope')\gexec

-- 3. Grant schema permissions
\c aquascope
GRANT ALL ON SCHEMA public TO aquascope_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aquascope_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aquascope_user;
