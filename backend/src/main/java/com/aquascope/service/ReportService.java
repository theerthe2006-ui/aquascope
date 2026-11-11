package com.aquascope.service;

import com.aquascope.entity.MonitoringSite;
import com.aquascope.entity.ParticleObservation;
import com.aquascope.entity.SampleAnalysis;
import com.aquascope.entity.WaterSample;
import com.aquascope.repository.MonitoringSiteRepository;
import com.aquascope.repository.ParticleObservationRepository;
import com.aquascope.repository.SampleAnalysisRepository;
import com.aquascope.repository.WaterSampleRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

@Service
public class ReportService {

    private final WaterSampleRepository sampleRepository;
    private final MonitoringSiteRepository siteRepository;
    private final SampleAnalysisRepository analysisRepository;
    private final ParticleObservationRepository observationRepository;

    public ReportService(
            WaterSampleRepository sampleRepository,
            MonitoringSiteRepository siteRepository,
            SampleAnalysisRepository analysisRepository,
            ParticleObservationRepository observationRepository
    ) {
        this.sampleRepository = sampleRepository;
        this.siteRepository = siteRepository;
        this.analysisRepository = analysisRepository;
        this.observationRepository = observationRepository;
    }

    @Transactional(readOnly = true)
    public String exportSamplesCsv() throws IOException {
        StringWriter out = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Sample Identifier", "Site Code", "Site Name", "Collection Date", "Volume (L)", "Data Origin", "Status", "Source")
                .build();

        try (CSVPrinter printer = new CSVPrinter(out, format)) {
            List<WaterSample> samples = sampleRepository.findAll();
            for (WaterSample s : samples) {
                printer.printRecord(
                        s.getId(),
                        s.getSampleIdentifier(),
                        s.getMonitoringSite() != null ? s.getMonitoringSite().getCode() : "N/A",
                        s.getMonitoringSite() != null ? s.getMonitoringSite().getName() : "N/A",
                        s.getCollectionDate(),
                        s.getSampleVolume(),
                        s.getDataOrigin(),
                        s.getStatus(),
                        s.getCollectionSource()
                );
            }
        }
        return out.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportSamplesPdf() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header
        Paragraph title = new Paragraph("AQUASCOPE — Water Sample Intelligence Report")
                .setFontSize(16)
                .setBold()
                .setFontColor(ColorConstants.DARK_GRAY);
        document.add(title);

        Paragraph metadata = new Paragraph("Generated at: " + Instant.now().toString() + " | Scientific Water Monitoring Platform")
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY);
        document.add(metadata);
        document.add(new Paragraph(" "));

        // Samples Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{15, 20, 20, 15, 15, 15})).useAllAvailableWidth();
        table.addHeaderCell(new Cell().add(new Paragraph("Identifier").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Monitoring Site").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Collection Date").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Volume").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Data Origin").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));

        List<WaterSample> samples = sampleRepository.findAll();
        for (WaterSample s : samples) {
            table.addCell(new Cell().add(new Paragraph(s.getSampleIdentifier()).setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(s.getMonitoringSite() != null ? s.getMonitoringSite().getCode() : "N/A").setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(s.getCollectionDate() != null ? s.getCollectionDate().toString().substring(0, 10) : "N/A").setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph((s.getSampleVolume() != null ? s.getSampleVolume().toString() : "0") + " " + (s.getVolumeUnit() != null ? s.getVolumeUnit() : "L")).setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(s.getDataOrigin() != null ? s.getDataOrigin().name() : "N/A").setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(s.getStatus() != null ? s.getStatus().name() : "N/A").setFontSize(9)));
        }

        document.add(table);
        document.close();
        return baos.toByteArray();
    }
}
