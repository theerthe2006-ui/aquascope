package com.aquascope.service;

import com.aquascope.entity.SampleImages;
import com.aquascope.repository.SampleImagesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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

    @Value("${app.upload.max-size-bytes:20971520}")
    private long maxFileSizeBytes;

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
     * Paginated, restartable, and idempotent migration of legacy LOCAL records to Cloudflare R2.
     * Enforces path containment, file size limits, and SHA-256 integrity validation of R2 objects.
     */
    public MigrationSummary migrateLegacyRecordsToR2(int pageSize) {
        if (s3Client == null || r2Bucket == null || r2Bucket.isBlank()) {
            throw new IllegalStateException("Cloudflare R2 client is not configured; cannot migrate legacy evidence.");
        }

        int effectivePageSize = pageSize > 0 && pageSize <= 500 ? pageSize : 50;
        int pageNumber = 0;
        int totalProcessed = 0;
        int migrated = 0;
        int failed = 0;
        int missingLocalFiles = 0;

        boolean hasMore = true;

        while (hasMore) {
            Pageable pageable = PageRequest.of(pageNumber, effectivePageSize);
            Page<SampleImages> page = sampleImagesRepository.findAll(pageable);

            List<SampleImages> localRecords = page.getContent().stream()
                    .filter(img -> "LOCAL".equals(img.getStorageBackend()))
                    .toList();

            for (SampleImages record : localRecords) {
                totalProcessed++;
                try {
                    String key = record.getObjectKey() != null ? record.getObjectKey() : record.getStoredFilePath();
                    Path localFile = localFallbackLocation.resolve(key.replace("evidence/", "")).normalize();

                    // Security: Verify normalized path does not escape the configured upload directory
                    if (!localFile.startsWith(localFallbackLocation)) {
                        log.error("Security violation: Path traversal detected in legacy record ID {}: {}", record.getId(), localFile);
                        failed++;
                        continue;
                    }

                    if (!Files.exists(localFile) || !Files.isReadable(localFile)) {
                        log.warn("Legacy evidence ID {} physical local file not found at: {}. Leaving record as LOCAL.", record.getId(), localFile);
                        missingLocalFiles++;
                        continue;
                    }

                    long fileSize = Files.size(localFile);
                    if (fileSize > maxFileSizeBytes) {
                        log.error("Legacy evidence ID {} file size {} exceeds max limit of {} bytes", record.getId(), fileSize, maxFileSizeBytes);
                        failed++;
                        continue;
                    }

                    byte[] fileBytes = Files.readAllBytes(localFile);
                    String localSha256 = calculateSha256(fileBytes);

                    if (record.getSha256Checksum() == null || record.getSha256Checksum().isBlank()) {
                        record.setSha256Checksum(localSha256);
                    } else if (!record.getSha256Checksum().equalsIgnoreCase(localSha256)) {
                        log.error("Legacy evidence ID {} local file SHA-256 does not match recorded checksum. Skipping upload.", record.getId());
                        failed++;
                        continue;
                    }

                    // Check if object already exists in R2 and verify its content integrity
                    boolean alreadyMigratedAndVerified = false;
                    try {
                        ResponseBytes<GetObjectResponse> existingObject = s3Client.getObjectAsBytes(
                                GetObjectRequest.builder().bucket(r2Bucket).key(key).build()
                        );
                        String remoteSha256 = calculateSha256(existingObject.asByteArray());
                        if (remoteSha256.equalsIgnoreCase(localSha256)) {
                            alreadyMigratedAndVerified = true;
                            log.info("Legacy evidence ID {} already exists in R2 with matching checksum.", record.getId());
                        }
                    } catch (NoSuchKeyException ignored) {
                        // Object not present in R2 yet
                    } catch (Exception ex) {
                        log.warn("Could not verify existing R2 object for ID {}: {}. Will re-upload.", record.getId(), ex.getMessage());
                    }

                    if (!alreadyMigratedAndVerified) {
                        log.info("Uploading legacy evidence ID {} to R2 bucket: {}, key: {}", record.getId(), r2Bucket, key);
                        s3Client.putObject(PutObjectRequest.builder()
                                        .bucket(r2Bucket)
                                        .key(key)
                                        .contentType(record.getContentType())
                                        .contentLength((long) fileBytes.length)
                                        .build(),
                                RequestBody.fromBytes(fileBytes));

                        // Read back from R2 to strictly verify content checksum before updating database metadata
                        ResponseBytes<GetObjectResponse> uploadedBytes = s3Client.getObjectAsBytes(
                                GetObjectRequest.builder().bucket(r2Bucket).key(key).build()
                        );
                        String uploadedSha256 = calculateSha256(uploadedBytes.asByteArray());
                        if (!uploadedSha256.equalsIgnoreCase(localSha256)) {
                            throw new IllegalStateException("Uploaded R2 object SHA-256 mismatch for ID: " + record.getId());
                        }
                    }

                    // Metadata update: only transition when content integrity is proven
                    record.setStorageBackend("R2");
                    record.setStorageStatus("AVAILABLE");
                    record.setSha256Checksum(localSha256);
                    sampleImagesRepository.save(record);
                    migrated++;
                } catch (Exception ex) {
                    log.error("Failed to migrate legacy evidence ID {}: {}", record.getId(), ex.getMessage(), ex);
                    failed++;
                }
            }

            hasMore = page.hasNext();
            pageNumber++;
        }

        return new MigrationSummary(totalProcessed, migrated, failed, missingLocalFiles);
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm error", e);
        }
    }

    public record MigrationSummary(int totalProcessed, int migrated, int failed, int missingLocalFiles) {}
}
