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
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final Environment env;

    @Value("${app.r2.bucket:aquascope-evidence}")
    private String r2Bucket;

    @Value("${app.upload.max-size-bytes:20971520}") // Default 20 MB
    private long maxFileSizeBytes;

    public EvidenceStorageService(
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            SampleImagesRepository sampleImagesRepository,
            WaterSampleRepository waterSampleRepository,
            SampleAnalysisRepository sampleAnalysisRepository,
            UserRepository userRepository,
            Environment env,
            @Autowired(required = false) S3Client s3Client
    ) {
        this.sampleImagesRepository = sampleImagesRepository;
        this.waterSampleRepository = waterSampleRepository;
        this.sampleAnalysisRepository = sampleAnalysisRepository;
        this.userRepository = userRepository;
        this.env = env;
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

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File size exceeds maximum permitted limit (" + (maxFileSizeBytes / (1024 * 1024)) + "MB).");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read evidence file bytes", e);
        }

        // Detect and validate real MIME type using magic numbers (never trust client header)
        String verifiedContentType = detectAndValidateSignature(fileBytes);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid file path or name detected (path traversal prevention).");
        }

        // Determine safe extension based solely on verified MIME type
        String extension = switch (verifiedContentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };

        String objectKey = "evidence/" + UUID.randomUUID().toString() + extension;
        String sha256Checksum = calculateSha256(fileBytes);

        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");

        if (s3Client != null && r2Bucket != null && !r2Bucket.isBlank()) {
            try {
                log.info("Uploading evidence object to Cloudflare R2 bucket: {}, key: {}", r2Bucket, objectKey);
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(r2Bucket)
                        .key(objectKey)
                        .contentType(verifiedContentType)
                        .contentLength((long) fileBytes.length)
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
            } catch (Exception e) {
                log.error("Failed to store evidence in Cloudflare R2: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Object storage upload failure", e);
            }
        } else {
            if (isProd) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Production storage unconfigured: R2 client unavailable.");
            }
            // Local development fallback
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
        image.setContentType(verifiedContentType);
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

        // Enforce authorization for the requested evidence image
        validateEvidenceAccess(image);

        String key = image.getObjectKey() != null ? image.getObjectKey() : image.getStoredFilePath();

        if (s3Client != null && r2Bucket != null && !r2Bucket.isBlank()) {
            try {
                log.info("Streaming evidence object from Cloudflare R2 bucket: {}, key: {}", r2Bucket, key);
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(r2Bucket)
                        .key(key)
                        .build();

                ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
                return new ByteArrayResource(objectBytes.asByteArray());
            } catch (NoSuchKeyException e) {
                log.warn("Evidence object missing from R2 bucket: {}", key);
                throw new ResourceNotFoundException("Evidence object not found in object storage: " + image.getFileName());
            } catch (Exception e) {
                log.error("R2 storage/network failure fetching object {}: {}", key, e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Object storage temporarily unavailable", e);
            }
        }

        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (isProd) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Production storage unconfigured: R2 client unavailable.");
        }

        // Local development fallback
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

        validateEvidenceAccess(image);

        String key = image.getObjectKey() != null ? image.getObjectKey() : image.getStoredFilePath();

        if (s3Client != null && r2Bucket != null && !r2Bucket.isBlank()) {
            try {
                log.info("Deleting evidence object from R2: {}", key);
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(r2Bucket).key(key).build());
            } catch (Exception e) {
                log.error("Failed to delete object from R2: {}. Preserving PostgreSQL metadata.", e.getMessage(), e);
                // Do not delete database record if R2 deletion fails; preserve metadata and throw error so caller can retry
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to delete file from object storage; metadata preserved. Please retry.", e);
            }
        } else {
            boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
            if (isProd) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Production storage unconfigured: R2 client unavailable.");
            }
            try {
                Path filePath = this.localFallbackStorageLocation.resolve(key.replace("evidence/", "")).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Failed to delete local fallback file: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete local file; metadata preserved.", e);
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
        SampleImages image = sampleImagesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + id));
        validateEvidenceAccess(image);
        return image;
    }

    private void validateEvidenceAccess(SampleImages image) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required to access evidence.");
        }

        boolean hasElevatedRole = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") ||
                a.getAuthority().equals("ROLE_LABORATORY_RESEARCHER") ||
                a.getAuthority().equals("ROLE_REVIEWER") ||
                a.getAuthority().equals("ROLE_ENVIRONMENTAL_FIELD_OFFICER")
        );

        if (!hasElevatedRole) {
            throw new AccessDeniedException("Access denied: Insufficient role permissions to view or download scientific evidence.");
        }
    }

    private String detectAndValidateSignature(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new IllegalArgumentException("Unsupported or corrupt file format: File size too small for header detection.");
        }

        // JPEG Magic Number: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG Magic Number: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 &&
            bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }

        // WebP Magic Number: RIFF (bytes 0-3) and WEBP (bytes 8-11)
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
            bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }

        throw new IllegalArgumentException("Unsupported file format. Only verified JPEG, PNG, and WebP images are permitted.");
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
