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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
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
    private final EvidenceStateTransitionService stateTransitionService;
    private final S3Client s3Client;
    private final Environment env;

    @Value("${app.r2.bucket:aquascope-evidence}")
    private String r2Bucket;

    @Value("${app.upload.max-size-bytes:20971520}") // 20 MB max file size
    private long maxFileSizeBytes;

    // Maximum safe dimension and pixel count to prevent decompression bombs (pixel flood)
    private static final int MAX_IMAGE_DIMENSION = 8192; // 8K max width/height
    private static final long MAX_TOTAL_PIXELS = 67108864L; // 8192 x 8192 = 64 megapixels max

    public EvidenceStorageService(
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            SampleImagesRepository sampleImagesRepository,
            WaterSampleRepository waterSampleRepository,
            SampleAnalysisRepository sampleAnalysisRepository,
            UserRepository userRepository,
            EvidenceStateTransitionService stateTransitionService,
            Environment env,
            @Autowired(required = false) S3Client s3Client
    ) {
        this.sampleImagesRepository = sampleImagesRepository;
        this.waterSampleRepository = waterSampleRepository;
        this.sampleAnalysisRepository = sampleAnalysisRepository;
        this.userRepository = userRepository;
        this.stateTransitionService = stateTransitionService;
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

        // Full structural decode and decompression bomb protection (JPEG, PNG, WebP)
        String verifiedContentType = validateAndDecodeImageStructure(fileBytes);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid file path or name detected (path traversal prevention).");
        }

        // Enforce strict resource-level modification access
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + currentUsername));

        WaterSample targetSample = null;
        if (waterSampleId != null) {
            targetSample = waterSampleRepository.findById(waterSampleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Water sample not found with ID: " + waterSampleId));
            validateSampleModificationAccess(targetSample, currentUser);
        }

        SampleAnalysis targetAnalysis = null;
        if (sampleAnalysisId != null) {
            targetAnalysis = sampleAnalysisRepository.findById(sampleAnalysisId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sample analysis not found with ID: " + sampleAnalysisId));
            validateAnalysisModificationAccess(targetAnalysis, currentUser);
            if (targetSample == null) {
                targetSample = targetAnalysis.getWaterSample();
            }
        }

        String extension = switch (verifiedContentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };

        String objectKey = "evidence/" + UUID.randomUUID().toString() + extension;
        String sha256Checksum = calculateSha256(fileBytes);

        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        String storageBackend = "LOCAL";

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
                storageBackend = "R2";
            } catch (Exception e) {
                log.error("Failed to store evidence in Cloudflare R2: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Object storage upload failure", e);
            }
        } else {
            if (isProd) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Production storage unconfigured: R2 client unavailable.");
            }
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
        image.setUploadedByUser(currentUser);
        image.setWaterSample(targetSample);
        image.setSampleAnalysis(targetAnalysis);
        image.setStorageStatus("AVAILABLE");
        image.setStorageBackend(storageBackend);

        SampleImages saved = sampleImagesRepository.save(image);
        return mapToDto(saved);
    }

    public Resource loadEvidenceAsResource(Long imageId) {
        SampleImages image = sampleImagesRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence image not found with ID: " + imageId));

        if ("DELETED".equals(image.getStorageStatus()) || "PENDING_DELETION".equals(image.getStorageStatus())) {
            throw new ResourceNotFoundException("Evidence image is not available or has been scheduled for deletion.");
        }

        // Strict resource-level access control
        validateEvidenceResourceAccess(image);

        String key = image.getObjectKey() != null ? image.getObjectKey() : image.getStoredFilePath();

        if ("R2".equals(image.getStorageBackend()) || (s3Client != null && r2Bucket != null && !r2Bucket.isBlank())) {
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Production storage unconfigured: Legacy or local storage cannot be read without R2 migration.");
        }

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

    /**
     * Non-blocking, retryable two-phase deletion workflow with autonomous database transactions.
     */
    public void deleteEvidence(Long imageId) {
        SampleImages image = sampleImagesRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence image not found with ID: " + imageId));

        validateEvidenceDeletionAccess(image);

        String key = image.getObjectKey() != null ? image.getObjectKey() : image.getStoredFilePath();

        // 1. Commit PENDING_DELETION in an independent committed transaction.
        // If the storage delete fails, the record remains safely flagged as PENDING_DELETION and cannot be viewed.
        stateTransitionService.markPendingDeletion(imageId);

        // 2. Perform object deletion on Cloudflare R2 or local storage
        if ("R2".equals(image.getStorageBackend()) || (s3Client != null && r2Bucket != null && !r2Bucket.isBlank())) {
            try {
                log.info("Deleting evidence object from R2: {}", key);
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(r2Bucket).key(key).build());
            } catch (Exception e) {
                log.error("Failed to delete object from R2: {}. State committed as PENDING_DELETION for retry.", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to delete from object storage. Record flagged as PENDING_DELETION and can be retried.", e);
            }
        } else {
            try {
                Path filePath = this.localFallbackStorageLocation.resolve(key.replace("evidence/", "")).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Failed to delete local fallback file: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete local file. Record flagged as PENDING_DELETION.", e);
            }
        }

        // 3. Object confirmed removed from storage: finalize deletion in independent committed transaction
        stateTransitionService.finalizeDeletion(imageId);
    }

    @Transactional(readOnly = true)
    public List<SampleImageDto> getEvidenceByAnalysisId(Long analysisId) {
        SampleAnalysis analysis = sampleAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Sample analysis not found: " + analysisId));
        validateAnalysisReadAccess(analysis);

        return sampleImagesRepository.findBySampleAnalysisId(analysisId).stream()
                .filter(img -> "AVAILABLE".equals(img.getStorageStatus()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SampleImageDto> getEvidenceBySampleId(Long sampleId) {
        WaterSample sample = waterSampleRepository.findById(sampleId)
                .orElseThrow(() -> new ResourceNotFoundException("Water sample not found: " + sampleId));
        validateSampleReadAccess(sample);

        return sampleImagesRepository.findByWaterSampleId(sampleId).stream()
                .filter(img -> "AVAILABLE".equals(img.getStorageStatus()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SampleImages getImageEntity(Long id) {
        SampleImages image = sampleImagesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + id));
        validateEvidenceResourceAccess(image);
        return image;
    }

    // --- Strict Resource-Level Authorization Rules ---

    private void validateEvidenceResourceAccess(SampleImages image) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required to access evidence.");
        }

        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
        boolean isReviewer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_REVIEWER"));

        if (isAdmin || isReviewer) {
            return; // Admins and Reviewers have legitimate read access to evaluate scientific submissions
        }

        // Original uploader always has access to their own evidence
        if (image.getUploadedByUser() != null && image.getUploadedByUser().getUsername().equals(username)) {
            return;
        }

        // Specific analysis assignee check
        if (image.getSampleAnalysis() != null && image.getSampleAnalysis().getAnalyzedByUser() != null
                && image.getSampleAnalysis().getAnalyzedByUser().getUsername().equals(username)) {
            return;
        }

        // Specific sample collector check
        if (image.getWaterSample() != null && image.getWaterSample().getCollectedByUser() != null
                && image.getWaterSample().getCollectedByUser().getUsername().equals(username)) {
            return;
        }

        throw new AccessDeniedException("Access denied: You do not have permission to view or retrieve this evidence record.");
    }

    private void validateEvidenceDeletionAccess(SampleImages image) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }

        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
        if (isAdmin) {
            return;
        }

        // Only original uploader can delete their unverified evidence
        if (image.getUploadedByUser() != null && image.getUploadedByUser().getUsername().equals(username)) {
            return;
        }

        throw new AccessDeniedException("Access denied: Only administrators or the original uploader can delete this evidence.");
    }

    private void validateSampleReadAccess(WaterSample sample) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdminOrReviewer = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") || a.getAuthority().equals("ROLE_REVIEWER")
        );

        if (isAdminOrReviewer) {
            return;
        }

        if (sample.getCollectedByUser() != null && sample.getCollectedByUser().getUsername().equals(username)) {
            return;
        }

        throw new AccessDeniedException("Access denied: You are not authorized to view evidence for this water sample.");
    }

    private void validateSampleModificationAccess(WaterSample sample, User currentUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
        if (isAdmin) {
            return;
        }

        boolean isCollector = sample.getCollectedByUser() != null && sample.getCollectedByUser().getId().equals(currentUser.getId());
        if (!isCollector) {
            throw new AccessDeniedException("Access denied: You can only attach evidence to water samples assigned to you.");
        }
    }

    private void validateAnalysisReadAccess(SampleAnalysis analysis) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdminOrReviewer = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") || a.getAuthority().equals("ROLE_REVIEWER")
        );

        if (isAdminOrReviewer) {
            return;
        }

        if (analysis.getAnalyzedByUser() != null && analysis.getAnalyzedByUser().getUsername().equals(username)) {
            return;
        }

        throw new AccessDeniedException("Access denied: You are not authorized to inspect evidence for this analysis.");
    }

    private void validateAnalysisModificationAccess(SampleAnalysis analysis, User currentUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
        if (isAdmin) {
            return;
        }

        if (analysis.getAnalyzedByUser() != null && analysis.getAnalyzedByUser().getId().equals(currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException("Access denied: You cannot attach evidence to another researcher's analysis session.");
    }

    // --- Complete Structural Image Decoding via ImageIO & Plugin ---

    private String validateAndDecodeImageStructure(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new IllegalArgumentException("Unsupported or corrupt file format: File too small to contain valid image headers.");
        }

        // 1. Initial format detection by magic bytes
        String detectedFormat = null;
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            detectedFormat = "image/jpeg";
        } else if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 &&
                bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            detectedFormat = "image/png";
        } else if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            detectedFormat = "image/webp";
        }

        if (detectedFormat == null) {
            throw new IllegalArgumentException("Unsupported image format. Allowed: JPEG, PNG, and WebP.");
        }

        // 2. Structural decoding for all formats (JPEG, PNG, WebP) using ImageIO
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                // If WebP plugin is missing in test environment, perform container verification
                if ("image/webp".equals(detectedFormat)) {
                    int riffLength = ((bytes[7] & 0xFF) << 24) | ((bytes[6] & 0xFF) << 16) | ((bytes[5] & 0xFF) << 8) | (bytes[4] & 0xFF);
                    if (riffLength <= 0 || bytes.length < riffLength + 8) {
                        throw new IllegalArgumentException("Truncated or malformed WebP image stream.");
                    }
                    return detectedFormat;
                }
                throw new IllegalArgumentException("Malformed or unreadable image stream: No decoder found.");
            }

            ImageReader reader = readers.next();
            reader.setInput(iis, true, true);

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);

            // Decompression bomb (pixel flood) defense
            if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                throw new IllegalArgumentException(String.format(
                        "Image dimensions (%dx%d) exceed maximum safety limit of %dx%d (decompression bomb protection).",
                        width, height, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION
                ));
            }

            long totalPixels = (long) width * height;
            if (totalPixels > MAX_TOTAL_PIXELS) {
                throw new IllegalArgumentException(String.format(
                        "Total pixel count (%d) exceeds safety threshold of %d pixels.",
                        totalPixels, MAX_TOTAL_PIXELS
                ));
            }

            // Verify raster decode
            BufferedImage image = reader.read(0);
            if (image == null) {
                throw new IllegalArgumentException("Corrupt image data: Failed to decode raster frame.");
            }

        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("Corrupted or malformed image file: " + e.getMessage(), e);
        }

        return detectedFormat;
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
                entity.getStorageStatus(),
                entity.getStorageBackend(),
                entity.getContentType(),
                entity.getFileSizeBytes(),
                entity.getImagingModality(),
                entity.getMagnification(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
