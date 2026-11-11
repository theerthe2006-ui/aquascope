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
