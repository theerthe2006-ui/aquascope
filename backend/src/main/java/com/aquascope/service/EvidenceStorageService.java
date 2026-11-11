package com.aquascope.service;

import com.aquascope.dto.SampleImageDto;
import com.aquascope.entity.SampleAnalysis;
import com.aquascope.entity.SampleImages;
import com.aquascope.entity.User;
import com.aquascope.entity.WaterSample;
import com.aquascope.exception.ResourceNotFoundException;
import com.aquascope.repository.SampleAnalysisRepository;
import com.aquascope.repository.SampleImagesRepository;
import com.aquascope.repository.UserRepository;
import com.aquascope.repository.WaterSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvidenceStorageService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceStorageService.class);

    private final Path localFallbackStorageLocation;
    private final SampleImagesRepository sampleImagesRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final SampleAnalysisRepository sampleAnalysisRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;

    @Value("${app.r2.bucket:aquascope-evidence}")
    private String r2Bucket;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/tiff",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB

    public EvidenceStorageService(
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            SampleImagesRepository sampleImagesRepository,
            WaterSampleRepository waterSampleRepository,
            SampleAnalysisRepository sampleAnalysisRepository,
            UserRepository userRepository,
            @Autowired(required = false) S3Client s3Client
    ) {
        this.sampleImagesRepository = sampleImagesRepository;
        this.waterSampleRepository = waterSampleRepository;
        this.sampleAnalysisRepository = sampleAnalysisRepository;
        this.userRepository = userRepository;
        this.s3Client = s3Client;
        this.localFallbackStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.localFallbackStorageLocation);
        } catch (IOException ex) {
            log.warn("Could not create local fallback directory: {}", ex.getMessage());
        }
    }

    @Transactional
    public SampleImageDto storeEvidence(
            MultipartFile file,
            Long waterSampleId,
            Long sampleAnalysisId,
            String imagingModality,
            String magnification,
            String description
    ) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty evidence file.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 20MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type: " + contentType + ". Allowed: JPEG, PNG, TIFF, WebP.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid file path or name detected (path traversal prevention).");
        }

        // Validate actual file magic bytes (signatures)
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read evidence file bytes", e);
        }

        validateFileSignature(fileBytes, contentType);

        // Compute SHA-256 Checksum for evidence integrity
        String sha256Checksum = calculateSha256(fileBytes);

        // Clean extension and generate secure UUID object key
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String objectKey = "evidence/" + UUID.randomUUID().toString() + extension;

        // Store to Cloudflare R2 if configured, else store to local fallback
        if (s3Client != null && r2Bucket != null && !r2Bucket.isBlank()) {
            try {
                log.info("Uploading evidence object to Cloudflare R2 bucket: {}, key: {}", r2Bucket, objectKey);
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(r2Bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .contentLength((long) fileBytes.length)
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
            } catch (Exception e) {
                log.error("Failed to store evidence in Cloudflare R2: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload evidence to secure object storage", e);
            }
        } else {
            // Local fallback
            try {
                Path targetLocation = this.localFallbackStorageLocation.resolve(objectKey.replace("evidence/", "")).normalize();
                if (!targetLocation.startsWith(this.localFallbackStorageLocation)) {
                    throw new SecurityException("Attempted path traversal write attack.");
                }
                Files.write(targetLocation, fileBytes);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to store file on local disk fallback.", ex);
            }
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        SampleImages image = new SampleImages();
        image.setFileName(originalFilename);
        image.setStoredFilePath(objectKey);
        image.setObjectKey(objectKey);
        image.setSha256Checksum(sha256Checksum);
        image.setContentType(contentType);
        image.setFileSizeBytes((long) fileBytes.length);
        image.setImagingModality(imagingModality != null ? imagingModality : "BRIGHTFIELD_MICROSCOPY");
        image.setMagnification(magnification);
        image.setDescription(description);
        image.setUploadedByUser(user);

        if (waterSampleId != null) {
            WaterSample ws = waterSampleRepository.findById(waterSampleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Water sample not found: " + waterSampleId));
            image.setWaterSample(ws);
        }

        if (sampleAnalysisId != null) {
            SampleAnalysis sa = sampleAnalysisRepository.findById(sampleAnalysisId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sample analysis not found: " + sampleAnalysisId));
            image.setSampleAnalysis(sa);
        }

        SampleImages saved = sampleImagesRepository.save(image);
        return mapToDto(saved);
    }

    public Resource loadEvidenceAsResource(Long imageId) {
        SampleImages image = sampleImagesRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence image not found with ID: " + imageId));

        String key = image.getObjectKey() != null ? image.getObjectKey() : image.getStoredFilePath();

        // Retrieve from Cloudflare R2 if client is configured
        if (s3Client != null && r2Bucket != null && !r2Bucket.isBlank()) {
            try {
                log.info("Streaming evidence object from Cloudflare R2 bucket: {}, key: {}", r2Bucket, key);
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(r2Bucket)
                        .key(key)
                        .build();

                ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
                return new ByteArrayResource(objectBytes.asByteArray());
            } catch (Exception e) {
                log.error("Failed to fetch object from R2: {}", e.getMessage(), e);
                throw new ResourceNotFoundException("Evidence object not found in object storage: " + image.getFileName());
            }
        }

        // Local fallback
        try {
            Path filePath = this.localFallbackStorageLocation.resolve(key.replace("evidence/", "")).normalize();
            if (!filePath.startsWith(this.localFallbackStorageLocation)) {
                throw new SecurityException("Illegal file traversal attempt.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on filesystem: " + image.getFileName());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Error resolving evidence image path", ex);
        }
    }

    @Transactional
    public void deleteEvidence(Long imageId) {
        SampleImages image = sampleImagesRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence image not found with ID: " + imageId));

        String key = image.getObjectKey() != null ? image.getObjectKey() : image.getStoredFilePath();

        if (s3Client != null && r2Bucket != null && !r2Bucket.isBlank()) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(r2Bucket).key(key).build());
            } catch (Exception e) {
                log.warn("Failed to delete object from R2 bucket: {}", e.getMessage());
            }
        } else {
            try {
                Path filePath = this.localFallbackStorageLocation.resolve(key.replace("evidence/", "")).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete local fallback file: {}", e.getMessage());
            }
        }

        sampleImagesRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public List<SampleImageDto> getEvidenceByAnalysisId(Long analysisId) {
        return sampleImagesRepository.findBySampleAnalysisId(analysisId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SampleImageDto> getEvidenceBySampleId(Long sampleId) {
        return sampleImagesRepository.findByWaterSampleId(sampleId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SampleImages getImageEntity(Long id) {
        return sampleImagesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + id));
    }

    private void validateFileSignature(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length < 4) {
            throw new IllegalArgumentException("File is corrupted or too small.");
        }

        // JPEG: FF D8 FF
        if (contentType.equalsIgnoreCase("image/jpeg")) {
            if ((bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8 || (bytes[2] & 0xFF) != 0xFF) {
                throw new IllegalArgumentException("File content does not match JPEG signature.");
            }
        }
        // PNG: 89 50 4E 47
        else if (contentType.equalsIgnoreCase("image/png")) {
            if ((bytes[0] & 0xFF) != 0x89 || bytes[1] != 0x50 || bytes[2] != 0x4E || bytes[3] != 0x47) {
                throw new IllegalArgumentException("File content does not match PNG signature.");
            }
        }
        // WebP: RIFF ... WEBP
        else if (contentType.equalsIgnoreCase("image/webp")) {
            if (bytes.length < 12 || bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F' || bytes[3] != 'F') {
                throw new IllegalArgumentException("File content does not match WebP RIFF signature.");
            }
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private SampleImageDto mapToDto(SampleImages entity) {
        return new SampleImageDto(
                entity.getId(),
                entity.getWaterSample() != null ? entity.getWaterSample().getId() : null,
                entity.getSampleAnalysis() != null ? entity.getSampleAnalysis().getId() : null,
                entity.getUploadedByUser() != null ? entity.getUploadedByUser().getId() : null,
                entity.getUploadedByUser() != null ? entity.getUploadedByUser().getUsername() : null,
                entity.getFileName(),
                entity.getStoredFilePath(),
                entity.getObjectKey(),
                entity.getSha256Checksum(),
                entity.getContentType(),
                entity.getFileSizeBytes(),
                entity.getImagingModality(),
                entity.getMagnification(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
