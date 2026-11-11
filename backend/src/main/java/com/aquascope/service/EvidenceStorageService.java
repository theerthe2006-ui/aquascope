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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvidenceStorageService {

    private final Path fileStorageLocation;
    private final SampleImagesRepository sampleImagesRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final SampleAnalysisRepository sampleAnalysisRepository;
    private final UserRepository userRepository;

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
            UserRepository userRepository
    ) {
        this.sampleImagesRepository = sampleImagesRepository;
        this.waterSampleRepository = waterSampleRepository;
        this.sampleAnalysisRepository = sampleAnalysisRepository;
        this.userRepository = userRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the upload directory at " + this.fileStorageLocation, ex);
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

        // Clean extension
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String storedFileName = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName).normalize();
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Attempted path traversal write attack.");
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file on disk.", ex);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        SampleImages image = new SampleImages();
        image.setFileName(originalFilename);
        image.setStoredFilePath(storedFileName);
        image.setContentType(contentType);
        image.setFileSizeBytes(file.getSize());
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

        try {
            Path filePath = this.fileStorageLocation.resolve(image.getStoredFilePath()).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Illegal file traversal attempt.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on filesystem: " + image.getFileName());
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File path invalid: " + ex.getMessage());
        }
    }

    public SampleImages getImageEntity(Long imageId) {
        return sampleImagesRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence image not found with ID: " + imageId));
    }

    public List<SampleImageDto> getEvidenceByAnalysisId(Long analysisId) {
        return sampleImagesRepository.findBySampleAnalysisId(analysisId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<SampleImageDto> getEvidenceBySampleId(Long sampleId) {
        return sampleImagesRepository.findByWaterSampleId(sampleId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
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
                entity.getContentType(),
                entity.getFileSizeBytes(),
                entity.getImagingModality(),
                entity.getMagnification(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
