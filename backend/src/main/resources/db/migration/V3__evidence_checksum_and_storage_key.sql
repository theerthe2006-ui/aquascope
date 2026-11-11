-- V3__evidence_checksum_and_storage_key.sql
-- Add object_key and sha256_checksum to sample_images for Cloudflare R2 object storage support

ALTER TABLE sample_images
ADD COLUMN IF NOT EXISTS object_key VARCHAR(500),
ADD COLUMN IF NOT EXISTS sha256_checksum VARCHAR(64);

-- Populate existing rows where stored_file_path is populated
UPDATE sample_images
SET object_key = stored_file_path
WHERE object_key IS NULL;
