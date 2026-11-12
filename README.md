# AQUASCOPE — Microplastic Intelligence & Water Monitoring Platform

AQUASCOPE is a scientific software platform for environmental monitoring teams, laboratory researchers, and water resource managers to record, review, analyse, and report microplastic observations in water bodies with strict scientific integrity, provenance tracking, and fine-grained resource-level permissions.

---

## Free-Tier Cloud Architecture

To ensure 100% free-tier operation without expiring trial databases or billable persistent disks, AQUASCOPE utilizes the following decoupled cloud architecture:

```
[Vercel Free Tier]
   └─ React 19 + TypeScript + Vite SPA
         │  (HTTPS / REST)
         ▼
[Render Free Web Service]
   └─ Spring Boot 3.4.3 Docker Container
         │
         ├─── (JDBC over TLS / HikariCP) ───► [Neon Serverless PostgreSQL Free Tier]
         │                                       - Scale-to-zero supported (Hikari idle evictions)
         │                                       - Flyway migrations: V1, V2, V3
         │                                       - SSL enforced (`sslmode=require`)
         │
         └─── (S3-Compatible API) ──────────► [Cloudflare R2 Standard Object Storage]
                                                 - 10GB free tier storage
                                                 - Private bucket (`aquascope-evidence`)
                                                 - TwelveMonkeys WebP / ImageIO decoding & SHA-256
                                                 - Zero egress fees
```

### Free-Tier Limits & Specifications

| Provider | Service | Free Tier Limits | AQUASCOPE Optimization |
|---|---|---|---|
| **Render** | Web Service | 512MB RAM, 0.1 CPU, spins down after 15m idle | Health-check endpoint `/api/v1/health` with fast cold-start |
| **Neon** | Serverless PostgreSQL | 0.5 GiB storage, scales to zero when inactive | Hikari `minimumIdle=0`, `connectionTimeout=30s`, `idleTimeout=60s` for smooth suspend/resume |
| **Cloudflare** | R2 Object Storage | 10 GB storage, 1M Class A ops/mo, 10M Class B ops/mo, **0$ egress** | Private bucket, streamed authenticated access, UUID keys |
| **Vercel** | Frontend Hosting | 100GB bandwidth/mo, unlimited preview deploys | Production-optimized static build (`dist/`) with SPA rewrites |

---

## Safe Evidence Deletion & Legacy Migration

### 1. Two-Phase Safe Deletion State Machine with Bounded Retries
Evidence deletion uses autonomous database transactions (`Propagation.REQUIRES_NEW`) to prevent state desynchronization:
1. `markPendingDeletion(id)`: Flags the database record as `PENDING_DELETION` and writes an audit log. The file is immediately blocked from viewing or downloading.
2. `deleteObject(key)`: Executes object deletion on Cloudflare R2 with up to 3 bounded retries.
3. If R2 fails across all 3 attempts, the operation returns an HTTP 503 (`SERVICE_UNAVAILABLE`). The database record remains securely committed as `PENDING_DELETION` and is retryable.
4. `finalizeDeletion(id)`: Purges the storage object, writes a permanent deletion audit record with full file metadata and checksum, and removes the entity.

### 2. Paginated Legacy Evidence Migration Procedure
Legacy records created prior to Flyway migration V3 are marked with `storage_backend = 'LOCAL'`:
* **Ephemeral Storage Warning:** In production (`SPRING_PROFILES_ACTIVE=prod`), Render containers run on an ephemeral filesystem where disk state is wiped on rebuilds. **Do not attempt to migrate legacy files from Render's ephemeral filesystem.**
* **Operator Migration Procedure:** Legacy migration must be run from an environment (such as the developer workstation or server) that actually houses the original files:
  1. Ensure the source images are present in the directory configured by `UPLOAD_DIR` (e.g. `./uploads`).
  2. The Spring Boot application exposes `LegacyEvidenceMigrationService`:
     ```java
     // Batched/paginated migration with strict path normalization & SHA-256 verification
     MigrationSummary summary = legacyMigrationService.migrateLegacyRecordsToR2(50);
     ```
  3. The utility verifies each physical file's normalized path, verifies it does not escape the upload root, reads its bytes, verifies its SHA-256 against R2 using `GetObject`, and updates the database record to `storage_backend = 'R2'` and `storage_status = 'AVAILABLE'`.
  4. Missing local files are safely skipped and reported without falsifying metadata.

---

## Database Management & Backup Procedures

### Safe Neon PostgreSQL Backup / Export
Since Neon free databases scale down when idle, export schemas and scientific data securely using `pg_dump`:

```bash
# Export full database with schema and data:
pg_dump "postgres://<user>:<password>@<neon-host>/aquascope?sslmode=require" \
  --format=custom \
  --file=aquascope_backup_$(date +%Y%m%d).dump

# Plain SQL data export:
pg_dump "postgres://<user>:<password>@<neon-host>/aquascope?sslmode=require" \
  --data-only \
  --file=aquascope_data_$(date +%Y%m%d).sql
```

### Database Restoration
```bash
pg_restore -d "postgres://<user>:<password>@<neon-host>/aquascope?sslmode=require" \
  --clean --if-exists aquascope_backup.dump
```

---

## Required Production Environment Variables

### Render Backend Service:
Configure these environment variables in your Render Web Service settings:

| Variable | Description | Example / Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `SPRING_DATASOURCE_URL` | Neon JDBC Connection String | `jdbc:postgresql://ep-xyz.us-east-2.aws.neon.tech/aquascope?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Neon Database User | `aquascope_owner` |
| `SPRING_DATASOURCE_PASSWORD` | Neon Database Password | *(from Neon console)* |
| `JWT_SECRET` | Cryptographic secret for signing JWTs | *(Auto-generated by Render blueprint)* |
| `CORS_ALLOWED_ORIGINS` | Permitted frontend origin | `https://aquascope.vercel.app` |
| `R2_ENDPOINT` | Cloudflare R2 S3 Endpoint | `https://<account-id>.r2.cloudflarestorage.com` |
| `R2_BUCKET` | Cloudflare R2 Private Bucket | `aquascope-evidence` |
| `R2_ACCESS_KEY_ID` | Cloudflare R2 Token Access Key ID | *(from Cloudflare R2 dashboard)* |
| `R2_SECRET_ACCESS_KEY` | Cloudflare R2 Token Secret Key | *(from Cloudflare R2 dashboard)* |
| `R2_REGION` | Cloudflare S3 Region | `auto` |

### Vercel Frontend Service:
| Variable | Description | Value |
|---|---|---|
| `VITE_API_BASE_URL` | Production Backend API URL | `https://<your-service>.onrender.com/api/v1` |

---

## Production Deployment Guide

Follow this step-by-step checklist to deploy the complete AQUASCOPE platform to production across Neon PostgreSQL, Cloudflare R2, Render, and Vercel.

### Step 1: Create Neon Serverless PostgreSQL Database
1. Sign in to your [Neon Console](https://console.neon.tech).
2. Create a new project named `aquascope`.
3. Under the **Dashboard**, locate your connection details. Copy the PostgreSQL connection URI.
4. Prepare your JDBC URL by appending `?sslmode=require` to the hostname:
   `jdbc:postgresql://<neon-host>/aquascope?sslmode=require`
5. Note the database username and password.

### Step 2: Create Cloudflare R2 Private Bucket and API Token
1. Sign in to your [Cloudflare Dashboard](https://dash.cloudflare.com) and navigate to **R2**.
2. Click **Create bucket** and name it `aquascope-evidence` (or your preferred name). Ensure bucket visibility remains **Private** (never enable public access).
3. Under **Account Details**, copy your Account ID. The S3 endpoint URL will be:
   `https://<account-id>.r2.cloudflarestorage.com`
4. Navigate to **Manage R2 API Tokens** and click **Create API Token**.
5. Select permissions: **Object Read & Write** scoped to your `aquascope-evidence` bucket.
6. Copy the **Access Key ID** and **Secret Access Key**.

### Step 3: Deploy Backend on Render
1. Sign in to [Render](https://dashboard.render.com).
2. Click **New +** -> **Web Service** and connect your GitHub repository (`theerthe2006-ui/aquascope`).
3. Select Docker runtime (or use the repository's `render.yaml` Blueprint).
   - Docker Context: `backend`
   - Dockerfile Path: `backend/Dockerfile`
   - Health Check Path: `/api/v1/health`
4. Set the following environment variables under **Environment**:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://<neon-host>/aquascope?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME`: `<your-neon-user>`
   - `SPRING_DATASOURCE_PASSWORD`: `<your-neon-password>`
   - `JWT_SECRET`: *(A secure 256-bit random base64 or hex string, at least 32 characters)*
   - `CORS_ALLOWED_ORIGINS`: `https://<your-vercel-domain>.vercel.app` *(update once frontend is deployed)*
   - `R2_ENDPOINT`: `https://<account-id>.r2.cloudflarestorage.com`
   - `R2_BUCKET`: `aquascope-evidence`
   - `R2_ACCESS_KEY_ID`: `<your-cloudflare-r2-access-key-id>`
   - `R2_SECRET_ACCESS_KEY`: `<your-cloudflare-r2-secret-access-key>`
   - `R2_REGION`: `auto`
5. Click **Create Web Service**. Wait for the build and deployment to complete, and verify `/api/v1/health` returns HTTP 200 `{"status":"UP"}`. Copy your Render service URL (e.g., `https://aquascope-backend.onrender.com`).

### Step 4: Deploy Frontend on Vercel
1. Sign in to [Vercel](https://vercel.com).
2. Click **Add New...** -> **Project** and import `theerthe2006-ui/aquascope`.
3. Set **Root Directory** to `frontend`.
4. Framework Preset: **Vite**.
5. Under **Environment Variables**, add:
   - `VITE_API_BASE_URL`: `https://<your-render-service>.onrender.com/api/v1`
6. Click **Deploy**. Vercel will run `tsc -b && vite build` and deploy the SPA with client-side rewrites handled by `vercel.json`.
7. Once deployed, copy your production Vercel URL (e.g. `https://aquascope.vercel.app`).

### Step 5: Finalize CORS and Verify User Workflow
1. Return to the Render dashboard for `aquascope-backend`.
2. Update `CORS_ALLOWED_ORIGINS` to match your Vercel URL: `https://<your-vercel-domain>.vercel.app`.
3. Trigger a redeployment on Render (or wait for the environment update to apply).
4. Access your frontend on Vercel:
   - Verify health check connectivity in the status indicator.
   - Log in using your designated administrative account.
   - Verify role-based navigation, water sample registration, laboratory particle observation entries, microscopy evidence upload, review workflow, and PDF/CSV export.

---

## Security Hardening & Demo Account Management

> [!IMPORTANT]
> **Production Launch Notice:** Flyway migration `V2__seed_initial_users.sql` includes initial seed accounts for evaluation environments. Before public or untrusted production access:
> 1. Log in immediately as the administrator and rotate all user passwords or disable unused evaluation accounts via the user management API (`/api/v1/users/{id}`).
> 2. Alternatively, run the following SQL script against your production Neon database before launch to deactivate demonstration accounts:
>    ```sql
>    UPDATE users SET active = false WHERE username IN ('admin', 'researcher', 'field_officer', 'reviewer');
>    ```
> 3. Generate a dedicated administrative account with a strong, unguessable password.
> 4. Ensure `JWT_SECRET` is never committed to source control and is securely provided via cloud dashboard secrets.

---

## Local Development & Testing Instructions

### 1. Database Migrations
Migrations are managed automatically by Flyway upon application boot:
- `V1__initial_schema.sql`: 11 core tables (users, sites, samples, analyses, observations, devices, logs).
- `V2__seed_initial_users.sql`: Seeded role accounts with BCrypt-hashed passwords.
- `V3__evidence_checksum_and_storage_key.sql`: Schema enhancement for Cloudflare R2 object keys, SHA-256 integrity checksums, and storage states (`AVAILABLE`, `PENDING_DELETION`, `DELETED`).

### 2. Run Backend Tests
```powershell
cd backend
.\mvnw.cmd clean test
```

### 3. Build Frontend Production Artifacts
```powershell
cd frontend
npm run build
```
