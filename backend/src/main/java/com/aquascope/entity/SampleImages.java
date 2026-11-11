package com.aquascope.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sample_images")
public class SampleImages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_sample_id")
    private WaterSample waterSample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_analysis_id")
    private SampleAnalysis sampleAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedByUser;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "stored_file_path", nullable = false, length = 500)
    private String storedFilePath;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "sha256_checksum", length = 64)
    private String sha256Checksum;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "imaging_modality", nullable = false, length = 100)
    private String imagingModality = "BRIGHTFIELD_MICROSCOPY";

    @Column(length = 50)
    private String magnification;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public SampleImages() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WaterSample getWaterSample() { return waterSample; }
    public void setWaterSample(WaterSample waterSample) { this.waterSample = waterSample; }

    public SampleAnalysis getSampleAnalysis() { return sampleAnalysis; }
    public void setSampleAnalysis(SampleAnalysis sampleAnalysis) { this.sampleAnalysis = sampleAnalysis; }

    public User getUploadedByUser() { return uploadedByUser; }
    public void setUploadedByUser(User uploadedByUser) { this.uploadedByUser = uploadedByUser; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStoredFilePath() { return storedFilePath; }
    public void setStoredFilePath(String storedFilePath) { this.storedFilePath = storedFilePath; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getImagingModality() { return imagingModality; }
    public void setImagingModality(String imagingModality) { this.imagingModality = imagingModality; }

    public String getMagnification() { return magnification; }
    public void setMagnification(String magnification) { this.magnification = magnification; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }

    public Instant getCreatedAt() { return createdAt; }
}
