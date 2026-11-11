# AQUASCOPE — Microplastic Intelligence & Water Monitoring Platform

AQUASCOPE is a scientific software platform for environmental monitoring teams, laboratory researchers, and water resource managers to record, review, analyse, and report microplastic observations in water bodies.

---

## Architecture Overview

```
AQUASCOPE/
├── backend/                # Spring Boot 3.4.3 (Java 25 runtime, release 21 bytecode)
│   ├── src/main/java/com/aquascope/
│   │   ├── config/         # CORS and application configurations
│   │   ├── controller/     # REST controllers (HealthController, etc.)
│   │   ├── dto/            # Standardized ApiResponse and HealthResponse records
│   │   ├── entity/         # Domain entities (Users, MonitoringSites, etc.)
│   │   ├── exception/      # Global centralized exception handling
│   │   ├── repository/     # Spring Data JPA repositories
│   │   └── service/        # Business logic layer (HealthService, etc.)
│   ├── src/main/resources/
│   │   ├── application.yml # Production/dev configuration & datasource bindings
│   │   └── db/migration/   # Flyway SQL migrations (V1__initial_schema.sql)
│   └── src/test/           # JUnit 5 & Mockito integration tests
├── frontend/               # React 19 + TypeScript + Vite + Tailwind CSS
│   ├── src/
│   │   ├── components/     # Shell layout, Sidebar, Header with live health probe
│   │   ├── pages/          # Dashboard, Samples, Monitoring Sites, Analytics, Reports
│   │   ├── services/       # Axios API client (/api/v1)
│   │   └── types/          # Contract types matching backend DTOs
└── docs/                   # System design, API contracts, scientific workflows
```

---

## Technology Stack

- **Backend:** Java 25 (OpenJDK 25.0.3), Spring Boot 3.4.3, Spring Web, Spring Data JPA, Hibernate, PostgreSQL Driver, Flyway Migration, Actuator, JUnit 5, Maven Wrapper (`mvnw`).
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, Lucide React, React Router 7, Axios.
- **Database:** PostgreSQL 18.6 (Service: `postgresql-x64-18`, port `5432`, database: `aquascope`).

---

## Prerequisites

1. **Java JDK 21+** (JDK 25.0.3 verified)
2. **Node.js 20+** (v24.13.1 verified) & **npm 11+**
3. **PostgreSQL 18** running locally on port `5432`

---

## Environment Variables

### Backend Configuration
Configure these environment variables or provide them when running Spring Boot:

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC Connection URL | `jdbc:postgresql://localhost:5432/aquascope` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL User | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL Password | *(set by user)* |
| `SPRING_PROFILES_ACTIVE` | Active profile (`dev`, `test`, `prod`) | `dev` |
| `CORS_ALLOWED_ORIGINS` | Whitelisted frontend origins | `http://localhost:5173,http://localhost:3000` |

### Frontend Configuration
Create or edit `frontend/.env`:
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## Startup & Execution Instructions

### 1. Database Initialization
Ensure PostgreSQL has the `aquascope` database created:
```bash
# Using psql:
psql -U postgres -h localhost -c "CREATE DATABASE aquascope;"
```

### 2. Backend Startup
From the `backend` directory:
```powershell
cd backend

# On Windows:
$env:SPRING_DATASOURCE_PASSWORD="<your_postgres_password>"
.\mvnw.cmd spring-boot:run
```
The backend initializes on `http://localhost:8080`. Flyway automatically executes migrations from `classpath:db/migration`.

### 3. Running Backend Tests
```powershell
cd backend
.\mvnw.cmd clean test
```
The test suite runs against an isolated, in-memory PostgreSQL-compatible H2 instance with Flyway decoupled, leaving production data untouched.

### 4. Frontend Startup
From the `frontend` directory:
```powershell
cd frontend
npm install
npm run dev
```
Open your browser at `http://localhost:5173`.

### 5. Frontend Production Build Verification
```powershell
cd frontend
npm run build
```

---

## Core Health Endpoint Verification

- **GET** `/api/v1/health`
- **Response Shape:**
```json
{
  "success": true,
  "message": "Health status retrieved successfully",
  "data": {
    "status": "UP",
    "application": "aquascope-backend",
    "version": "1.0.0-phase1",
    "environment": "dev",
    "timestamp": "2026-11-11T18:00:00Z",
    "database": {
      "connected": true,
      "databaseType": "PostgreSQL",
      "details": "PostgreSQL 18.6"
    }
  },
  "timestamp": "2026-11-11T18:00:00Z",
  "errors": null
}
```
Credentials and connection strings are strictly shielded from all health responses.
