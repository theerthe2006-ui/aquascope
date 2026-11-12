package com.aquascope;

import com.aquascope.dto.*;
import com.aquascope.entity.*;
import com.aquascope.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ScientificWorkflowIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MonitoringSiteRepository monitoringSiteRepository;

    @Autowired
    private WaterSampleRepository waterSampleRepository;

    @Autowired
    private SampleAnalysisRepository sampleAnalysisRepository;

    @Autowired
    private ParticleObservationRepository particleObservationRepository;

    @Autowired
    private SampleImagesRepository sampleImagesRepository;

    @Autowired
    private AnalysisReviewRepository analysisReviewRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String adminToken;
    private String researcherToken;
    private String reviewerToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";

        auditLogRepository.deleteAll();
        analysisReviewRepository.deleteAll();
        sampleImagesRepository.deleteAll();
        particleObservationRepository.deleteAll();
        sampleAnalysisRepository.deleteAll();
        waterSampleRepository.deleteAll();
        monitoringSiteRepository.deleteAll();
        userRepository.deleteAll();

        // Seed users
        User admin = userRepository.save(new User(
                "sci_admin", "admin@sci.org", passwordEncoder.encode("Password123!"),
                "Dr. Administrator", Role.ADMINISTRATOR, "Quality Council"
        ));
        User researcher = userRepository.save(new User(
                "sci_researcher", "researcher@sci.org", passwordEncoder.encode("Password123!"),
                "Elena Researcher", Role.LABORATORY_RESEARCHER, "Spectroscopy Dept"
        ));
        User reviewer = userRepository.save(new User(
                "sci_reviewer", "reviewer@sci.org", passwordEncoder.encode("Password123!"),
                "Prof. Reviewer", Role.REVIEWER, "Peer Assurance Board"
        ));

        adminToken = authenticateAndGetToken("sci_admin", "Password123!");
        researcherToken = authenticateAndGetToken("sci_researcher", "Password123!");
        reviewerToken = authenticateAndGetToken("sci_reviewer", "Password123!");
    }

    private String authenticateAndGetToken(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNotNull();
        return response.getBody().data().token();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("Complete Scientific Provenance Workflow: Site -> Water Sample -> Lab Analysis -> Observation -> Submit -> Peer Review Approval")
    void completeEndToEndScientificWorkflow() {
        // 1. Create Monitoring Site
        MonitoringSiteDto siteDto = new MonitoringSiteDto(
                null,
                "SITE-TMR-01",
                "Thames Tidal Estuary Station",
                "Tidal baseline monitoring station",
                WaterSourceCategory.ESTUARY,
                new BigDecimal("51.5074"),
                new BigDecimal("-0.1278"),
                new BigDecimal("5.0"),
                null,
                true,
                null
        );

        ResponseEntity<ApiResponse<MonitoringSiteDto>> siteRes = restTemplate.exchange(
                baseUrl + "/monitoring-sites",
                HttpMethod.POST,
                new HttpEntity<>(siteDto, createAuthHeaders(adminToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(siteRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long siteId = siteRes.getBody().data().id();
        assertThat(siteId).isNotNull();

        // 2. Register Water Sample
        WaterSampleDto sampleDto = new WaterSampleDto(
                null,
                "AQ-2026-TMR-001",
                siteId,
                null,
                null,
                null,
                Instant.now(),
                new BigDecimal("2.5000"),
                "L",
                new BigDecimal("1.20"),
                new BigDecimal("7.40"),
                new BigDecimal("12.50"),
                new BigDecimal("8.20"),
                "SURFACE",
                "Clear, calm water",
                DataOrigin.MANUAL,
                SampleStatus.REGISTERED,
                "Collected using 300um stainless steel net",
                null
        );

        ResponseEntity<ApiResponse<WaterSampleDto>> sampleRes = restTemplate.exchange(
                baseUrl + "/water-samples",
                HttpMethod.POST,
                new HttpEntity<>(sampleDto, createAuthHeaders(adminToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(sampleRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long sampleId = sampleRes.getBody().data().id();
        assertThat(sampleId).isNotNull();

        // 3. Researcher creates Laboratory Analysis session
        SampleAnalysisDto analysisDto = new SampleAnalysisDto(
                null,
                sampleId,
                null,
                null,
                Instant.now(),
                "FTIR_SPECTROSCOPY",
                "PerkinElmer Spotlight 400",
                "100x",
                "WET_PEROXIDE_DIGESTION",
                "ANODISC",
                new BigDecimal("0.20"),
                0,
                null,
                AnalysisStatus.DRAFT,
                DataOrigin.MANUAL,
                "Spectroscopic polymer validation session",
                List.of(),
                null
        );

        ResponseEntity<ApiResponse<SampleAnalysisDto>> analysisRes = restTemplate.exchange(
                baseUrl + "/analyses",
                HttpMethod.POST,
                new HttpEntity<>(analysisDto, createAuthHeaders(researcherToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(analysisRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long analysisId = analysisRes.getBody().data().id();
        assertThat(analysisId).isNotNull();

        // 4. Add Particle Observation (with scientific polymer identification)
        ParticleObservationDto obsDto = new ParticleObservationDto(
                null,
                2,
                new BigDecimal("45.50"),
                new BigDecimal("80.00"),
                new BigDecimal("30.00"),
                ParticleShape.FRAGMENT,
                "BLUE",
                PolymerType.PE,
                IdentificationMethod.FTIR_MATCH,
                "HIGH",
                new BigDecimal("92.50"),
                "Polyethylene match verified against Bio-Rad KnowItAll library"
        );

        ResponseEntity<ApiResponse<ParticleObservationDto>> obsRes = restTemplate.exchange(
                baseUrl + "/analyses/" + analysisId + "/observations",
                HttpMethod.POST,
                new HttpEntity<>(obsDto, createAuthHeaders(researcherToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(obsRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 5. Submit for Peer Review
        ResponseEntity<ApiResponse<SampleAnalysisDto>> submitRes = restTemplate.exchange(
                baseUrl + "/analyses/" + analysisId + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(null, createAuthHeaders(researcherToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(submitRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitRes.getBody().data().status()).isEqualTo(AnalysisStatus.SUBMITTED_FOR_REVIEW);

        // 6. Reviewer conducts Peer Review and Approves
        AnalysisReviewDto reviewDto = new AnalysisReviewDto(
                null,
                null,
                null,
                "APPROVED",
                "Spectroscopic spectra verified. Polymer spectral match score 92.5% satisfies QA threshold (>80%).",
                null
        );

        ResponseEntity<ApiResponse<AnalysisReviewDto>> reviewRes = restTemplate.exchange(
                baseUrl + "/reviews/analysis/" + analysisId,
                HttpMethod.POST,
                new HttpEntity<>(reviewDto, createAuthHeaders(reviewerToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(reviewRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reviewRes.getBody().data().decision()).isEqualTo("APPROVED");

        // 7. Verify Water Sample Status updated to APPROVED
        ResponseEntity<ApiResponse<WaterSampleDto>> verifiedSampleRes = restTemplate.exchange(
                baseUrl + "/water-samples/" + sampleId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(reviewerToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(verifiedSampleRes.getBody().data().status()).isEqualTo(SampleStatus.APPROVED);
    }
}
