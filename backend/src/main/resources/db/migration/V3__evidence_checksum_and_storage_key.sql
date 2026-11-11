-- V3__evidence_checksum_and_storage_key.sql
-- Add object_key, sha256_checksum, and deletion_status to sample_images for Cloudflare R2 object storage support

ALTER TABLE sample_images
ADD COLUMN IF NOT EXISTS object_key VARCHAR(500),
ADD COLUMN IF NOT EXISTS sha256_checksum VARCHAR(64),
ADD COLUMN IF NOT EXISTS storage_status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, PENDING_DELETION, DELETED, STORAGE_ERROR
ADD COLUMN IF NOT EXISTS storage_backend VARCHAR(50) NOT NULL DEFAULT 'LOCAL'; -- LOCAL, R2

-- For legacy records created prior to V3, mark backend as LOCAL and leave sha256_checksum as NULL (do not fabricate fake hashes)
UPDATE sample_images
SET storage_backend = 'LOCAL'
WHERE storage_backend IS NULL;

-- Populate object_key from stored_file_path if null
UPDATE sample_images
SET object_key = stored_file_path
WHERE object_key IS NULL;
