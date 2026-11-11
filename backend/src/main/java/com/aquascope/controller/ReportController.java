package com.aquascope.controller;

import com.aquascope.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports & Export", description = "Endpoints for generating verifiable CSV and PDF reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(value = "/samples/csv", produces = "text/csv")
    @Operation(summary = "Export registered water samples to CSV")
    public ResponseEntity<String> exportSamplesCsv() throws IOException {
        String csvData = reportService.exportSamplesCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aquascope-samples.csv\"")
                .body(csvData);
    }

    @GetMapping(value = "/samples/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Export registered water samples to formatted PDF report")
    public ResponseEntity<byte[]> exportSamplesPdf() throws IOException {
        byte[] pdfBytes = reportService.exportSamplesPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aquascope-samples-report.pdf\"")
                .body(pdfBytes);
    }
}
