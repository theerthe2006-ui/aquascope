package com.aquascope.controller;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.SampleImageDto;
import com.aquascope.entity.SampleImages;
import com.aquascope.service.EvidenceStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evidence")
@Tag(name = "Microscopy Evidence", description = "Endpoints for uploading and retrieving high-resolution laboratory micrographs and spectra")
public class EvidenceController {

    private final EvidenceStorageService evidenceStorageService;

    public EvidenceController(EvidenceStorageService evidenceStorageService) {
        this.evidenceStorageService = evidenceStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'LABORATORY_RESEARCHER', 'ENVIRONMENTAL_FIELD_OFFICER')")
    @Operation(summary = "Upload microscopy or camera evidence for a sample or analysis")
    public ResponseEntity<ApiResponse<SampleImageDto>> uploadEvidence(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "waterSampleId", required = false) Long waterSampleId,
            @RequestParam(value = "sampleAnalysisId", required = false) Long sampleAnalysisId,
            @RequestParam(value = "imagingModality", required = false) String imagingModality,
            @RequestParam(value = "magnification", required = false) String magnification,
            @RequestParam(value = "description", required = false) String description
    ) {
        SampleImageDto stored = evidenceStorageService.storeEvidence(
                file, waterSampleId, sampleAnalysisId, imagingModality, magnification, description
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(stored, "Evidence uploaded and verified successfully"));
    }

    @GetMapping("/analysis/{analysisId}")
    @Operation(summary = "Get list of evidence files associated with an analysis session")
    public ResponseEntity<ApiResponse<List<SampleImageDto>>> getByAnalysis(@PathVariable Long analysisId) {
        List<SampleImageDto> list = evidenceStorageService.getEvidenceByAnalysisId(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(list, "Analysis evidence records retrieved"));
    }

    @GetMapping("/sample/{sampleId}")
    @Operation(summary = "Get list of evidence files associated with a water sample")
    public ResponseEntity<ApiResponse<List<SampleImageDto>>> getBySample(@PathVariable Long sampleId) {
        List<SampleImageDto> list = evidenceStorageService.getEvidenceBySampleId(sampleId);
        return ResponseEntity.ok(ApiResponse.ok(list, "Sample evidence records retrieved"));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download or stream evidence image file")
    public ResponseEntity<Resource> downloadEvidence(@PathVariable Long id) {
        SampleImages entity = evidenceStorageService.getImageEntity(id);
        Resource resource = evidenceStorageService.loadEvidenceAsResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(entity.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + entity.getFileName() + "\"")
                .body(resource);
    }
}
