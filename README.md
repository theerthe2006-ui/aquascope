# AQUASCOPE — Microplastic Intelligence & Water Monitoring Platform

AQUASCOPE is a scientific software platform for environmental monitoring teams, laboratory researchers, and water resource managers to record, review, analyse, and report microplastic observations in water bodies with strict scientific integrity and provenance tracking.

---

## Architecture Overview

```
AQUASCOPE/
├── backend/                # Spring Boot 3.4.3 (Java 21/25 runtime, Docker containerized)
│   ├── Dockerfile          # Multi-stage production Docker build (Eclipse Temurin 21)
│   ├── src/main/java/com/aquascope/
│   │   ├── config/         # CORS, Security, and Dynamic JDBC Database configuration
│   │   ├── controller/     # REST controllers (Auth, Sites, Samples, Analysis, Evidence, Reviews, Devices, Dashboard, Reports, Health)
│   │   ├── dto/            # Standardized API response records and payloads
│   │   ├── entity/         # Domain entities (Users, Sites, WaterSamples, Analyses, Evidence, Reviews, Devices, SensorReadings)
│   │   ├── exception/      # Centralized exception handling
│   │   ├── repository/     # Spring Data JPA repositories
│   │   ├── security/       # JWT token authentication and role-based access control
│   │   └── service/        # Scientific workflow and business logic services
│   ├── src/main/resources/
│   │   ├── application.yml # Production/dev configuration & datasource bindings
│   │   └── db/migration/   # Flyway SQL migrations:
│   │       ├── V1__initial_schema.sql       # 11 core scientific schema tables
│   │       └── V2__seed_initial_users.sql   # Seeded role-based user accounts
│   └── src/test/           # JUnit 5 & Mockito integration tests
├── frontend/               # React 19 + TypeScript + Vite + Tailwind CSS
│   ├── vercel.json         # Production SPA route rewrite configuration
│   ├── src/
│   │   ├── components/     # Layout, Navigation, Header with live health probe, ProtectedRoute
│   │   ├── context/        # AuthContext for JWT and role-based permissions
│   │   ├── pages/          # Dashboard, Locations, Samples, Analysis, Devices, Audit, Analytics, Reports, Settings
│   │   ├── services/       # Axios API client (/api/v1) with Bearer token interceptor
│   │   └── types/          # Strict TypeScript contract interfaces matching backend DTOs
└── render.yaml             # Render Blueprint definition (Managed PostgreSQL + Web Service Docker + Persistent Disk)
```

---

## Core Scientific Principles & Data Integrity

1. **Explicit Data Origin Tracking:** All measurements and observations are strictly categorized (`MANUAL`, `DEVICE`, `SIMULATED`). Simulated values are never conflated with real experimental measurements.
2. **Spectroscopic Verification:** Physical particle counts (microscopy observations) are clearly distinguished from verified polymer identifications (FTIR, Raman spectroscopy).
3. **Sensor Demarcation:** Physical environmental parameters (pH, turbidity, dissolved oxygen, temperature) recorded by deployed field sensors are kept separate from laboratory microplastic analysis.
4. **Peer Review Audit Trail:** Analyses require formal peer review (approval or rejection with reviewer rationale and immutable timestamps) before being finalized for reporting.

---

## Technology Stack

- **Backend:** Java 21/25 (Spring Boot 3.4.3, Spring Web, Spring Data JPA, Hibernate, Flyway, JWT, Actuator, JUnit 5).
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, Lucide React, React Router 7, Axios, Recharts.
- **Database:** PostgreSQL 18+ (Production: Render PostgreSQL; Test: in-memory H2 with PostgreSQL compatibility mode).
- **Deployment:** Render (Backend via Docker & Managed PostgreSQL with 1GB persistent disk for microscopy evidence), Vercel (Frontend SPA).

---

## Production Deployment (Render + Vercel)

### 1. Backend & Database (Render Blueprint)
The repository contains `render.yaml` declaring:
- **`aquascope-db`**: Free-tier managed PostgreSQL instance.
- **`aquascope-backend`**: Dockerized Spring Boot service built via `backend/Dockerfile` with persistent disk mounted at `/var/data/aquascope/uploads` for evidence images.
- Automatic conversion from Render's `postgres://` connection string to JDBC `jdbc:postgresql://` format via `DatabaseConfig`.
- Health check path set to `/api/v1/health`.

### 2. Frontend (Vercel)
- Root directory: `frontend`
- Build command: `npm run build`
- Output directory: `dist`
- Configured environment variable: `VITE_API_BASE_URL` pointing to `https://<backend-service>.onrender.com/api/v1`.
- SPA rewrites handled by `frontend/vercel.json`.

---

## Local Development & Testing Instructions

### 1. Database Initialization
```bash
psql -U postgres -h localhost -c "CREATE DATABASE aquascope;"
```

### 2. Run Backend Tests
```powershell
cd backend
.\mvnw.cmd clean test
```
*Executes unit and scientific workflow integration tests covering authentication, RBAC, sample creation, evidence handling, and reviews.*

### 3. Run Backend Locally
```powershell
cd backend
$env:SPRING_DATASOURCE_PASSWORD="your_password"
.\mvnw.cmd spring-boot:run
```

### 4. Run Frontend Locally
```powershell
cd frontend
npm install
npm run dev
```

### 5. Frontend Production Build Verification
```powershell
cd frontend
npm run build
```
