package com.aquascope.service;

import com.aquascope.entity.SampleImages;
import com.aquascope.repository.SampleImagesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class LegacyEvidenceMigrationService {

    private static final Logger log = LoggerFactory.getLogger(LegacyEvidenceMigrationService.class);

    private final SampleImagesRepository sampleImagesRepository;
    private final S3Client s3Client;
    private final Path localFallbackLocation;

    @Value("${app.r2.bucket:aquascope-evidence}")
    private String r2Bucket;

    public LegacyEvidenceMigrationService(
            SampleImagesRepository sampleImagesRepository,
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            @Autowired(required = false) S3Client s3Client
    ) {
        this.sampleImagesRepository = sampleImagesRepository;
        this.s3Client = s3Client;
        this.localFallbackLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Migrate all legacy records marked with storageBackend = 'LOCAL' to Cloudflare R2.
     * Preserves original record ID, sample associations, and computes SHA-256 if previously null.
     */
    public MigrationSummary migrateLegacyRecordsToR2() {
        if (s3Client == null || r2Bucket == null || r2Bucket.isBlank()) {
            throw new IllegalStateException("Cloudflare R2 client is not configured; cannot migrate legacy evidence.");
        }

        List<SampleImages> localRecords = sampleImagesRepository.findAll().stream()
                .filter(img -> "LOCAL".equals(img.getStorageBackend()))
                .toList();

        int successful = 0;
        int failed = 0;
        int skipped = 0;

        for (SampleImages record : localRecords) {
            try {
                String key = record.getObjectKey() != null ? record.getObjectKey() : record.getStoredFilePath();
                Path localFile = localFallbackLocation.resolve(key.replace("evidence/", "")).normalize();

                if (!Files.exists(localFile) || !Files.isReadable(localFile)) {
                    log.warn("Legacy evidence ID {} local file not found at: {}", record.getId(), localFile);
                    skipped++;
                    continue;
                }

                byte[] fileBytes = Files.readAllBytes(localFile);

                // Compute SHA-256 checksum if not set
                if (record.getSha256Checksum() == null || record.getSha256Checksum().isBlank()) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    record.setSha256Checksum(HexFormat.of().formatHex(digest.digest(fileBytes)));
                }

                // Check if already in R2
                boolean existsInR2 = false;
                try {
                    s3Client.headObject(HeadObjectRequest.builder().bucket(r2Bucket).key(key).build());
                    existsInR2 = true;
                } catch (NoSuchKeyException ignored) {
                    // Object does not exist yet
                }

                if (!existsInR2) {
                    log.info("Migrating legacy evidence ID {} to R2 bucket: {}, key: {}", record.getId(), r2Bucket, key);
                    s3Client.putObject(PutObjectRequest.builder()
                                    .bucket(r2Bucket)
                                    .key(key)
                                    .contentType(record.getContentType())
                                    .contentLength((long) fileBytes.length)
                                    .build(),
                            RequestBody.fromBytes(fileBytes));
                }

                // Verify upload succeeded via HeadObject before updating metadata
                s3Client.headObject(HeadObjectRequest.builder().bucket(r2Bucket).key(key).build());

                // Update database metadata to point to R2
                record.setStorageBackend("R2");
                record.setStorageStatus("AVAILABLE");
                sampleImagesRepository.save(record);
                successful++;
            } catch (Exception ex) {
                log.error("Failed to migrate legacy evidence ID {}: {}", record.getId(), ex.getMessage(), ex);
                failed++;
            }
        }

        return new MigrationSummary(localRecords.size(), successful, failed, skipped);
    }

    public record MigrationSummary(int totalFound, int migrated, int failed, int missingLocalFiles) {}
}
