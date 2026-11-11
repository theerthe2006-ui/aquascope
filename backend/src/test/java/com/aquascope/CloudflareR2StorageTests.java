package com.aquascope;

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
import com.aquascope.service.EvidenceStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class CloudflareR2StorageTests {

    @MockBean
    private S3Client s3Client;

    @Autowired
    private EvidenceStorageService evidenceStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SampleImagesRepository sampleImagesRepository;

    @Autowired
    private WaterSampleRepository waterSampleRepository;

    @Autowired
    private SampleAnalysisRepository sampleAnalysisRepository;

    private byte[] createTestImage(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, format, baos);
        if (!written && "webp".equalsIgnoreCase(format)) {
            // Minimal valid 1x1 lossy WebP frame if TwelveMonkeys writer is absent in test classpath
            return new byte[]{
                    'R', 'I', 'F', 'F', 0x1A, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P',
                    'V', 'P', '8', ' ', 0x0E, 0x00, 0x00, 0x00,
                    (byte) 0x9D, 0x01, 0x2A, 0x01, 0x00, 0x01, 0x00, 0x00, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00
            };
        }
        return baos.toByteArray();
    }

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("researcher", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_LABORATORY_RESEARCHER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should successfully upload PNG image to Cloudflare R2 with structural check and SHA-256 checksum")
    void shouldUploadImageToR2WithChecksum() throws IOException {
        byte[] pngBytes = createTestImage("png", 100, 100);
        MockMultipartFile file = new MockMultipartFile(
                "file", "microscope_bead.png", "image/png", pngBytes
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        SampleImageDto result = evidenceStorageService.storeEvidence(
                file, null, null, "BRIGHTFIELD_MICROSCOPY", "40x", "Fluorescent bead observation"
        );

        assertNotNull(result);
        assertNotNull(result.id());
        assertNotNull(result.objectKey());
        assertTrue(result.objectKey().startsWith("evidence/"));
        assertTrue(result.objectKey().endsWith(".png"));
        assertNotNull(result.sha256Checksum());
        assertEquals(64, result.sha256Checksum().length());
        assertEquals("image/png", result.contentType());
        assertEquals("AVAILABLE", result.storageStatus());

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(putCaptor.capture(), any(RequestBody.class));
        assertEquals(result.objectKey(), putCaptor.getValue().key());
    }

    @Test
    @DisplayName("Should reject malformed image bytes even if declared as PNG")
    void shouldRejectMalformedImageBytes() {
        byte[] corruptBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x00, 0x00, 0x00, 0x00
        };
        MockMultipartFile fakePng = new MockMultipartFile(
                "file", "corrupt.png", "image/png", corruptBytes
        );

        assertThrows(IllegalArgumentException.class, () -> {
            evidenceStorageService.storeEvidence(fakePng, null, null, null, null, null);
        });

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should reject malformed or truncated WebP image")
    void shouldRejectMalformedWebPImage() {
        // Starts with RIFF and WEBP but truncated before VP8 chunks
        byte[] truncatedWebp = new byte[]{
                'R', 'I', 'F', 'F', 0x14, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P',
                'V', 'P', '8', ' '
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "truncated.webp", "image/webp", truncatedWebp
        );

        assertThrows(IllegalArgumentException.class, () -> {
            evidenceStorageService.storeEvidence(file, null, null, null, null, null);
        });
    }

    @Test
    @DisplayName("Should reject oversized image exceeding upload limit")
    void shouldRejectOversizedImage() {
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x00, 0x00}
        ) {
            @Override
            public long getSize() {
                return 30 * 1024 * 1024; // 30 MB
            }
        };

        assertThrows(IllegalArgumentException.class, () -> {
            evidenceStorageService.storeEvidence(largeFile, null, null, null, null, null);
        });
    }

    @Test
    @DisplayName("Should stream stored object from R2 via authenticated backend endpoint")
    void shouldRetrieveAndStreamObjectFromR2() {
        byte[] sampleData = "scientific-binary-content".getBytes();
        GetObjectResponse getResponse = GetObjectResponse.builder().build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(getResponse, sampleData);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("spectrum.png");
        image.setStoredFilePath("evidence/test-spectrum.png");
        image.setObjectKey("evidence/test-spectrum.png");
        image.setContentType("image/png");
        image.setFileSizeBytes((long) sampleData.length);
        image.setUploadedByUser(researcher);
        image.setStorageBackend("R2");
        SampleImages saved = sampleImagesRepository.save(image);

        Resource resource = evidenceStorageService.loadEvidenceAsResource(saved.getId());
        assertNotNull(resource);
        assertTrue(resource.exists());

        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should return 404 ResourceNotFoundException when R2 object does not exist")
    void shouldReturn404WhenObjectNotFoundInR2() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("missing.png");
        image.setStoredFilePath("evidence/missing.png");
        image.setObjectKey("evidence/missing.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        image.setStorageBackend("R2");
        SampleImages saved = sampleImagesRepository.save(image);

        assertThrows(ResourceNotFoundException.class, () -> {
            evidenceStorageService.loadEvidenceAsResource(saved.getId());
        });
    }

    @Test
    @DisplayName("Should return 503 ResponseStatusException when temporary R2 outage occurs")
    void shouldReturn503WhenR2OutageOccurs() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("Cloudflare R2 500 Connection reset"));

        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("outage.png");
        image.setStoredFilePath("evidence/outage.png");
        image.setObjectKey("evidence/outage.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        image.setStorageBackend("R2");
        SampleImages saved = sampleImagesRepository.save(image);

        assertThrows(ResponseStatusException.class, () -> {
            evidenceStorageService.loadEvidenceAsResource(saved.getId());
        });
    }

    @Test
    @DisplayName("Resource-level authorization: Unrelated user cannot download evidence by guessing image ID")
    void shouldEnforceResourceLevelAuthorizationForImageDownload() {
        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("confidential.png");
        image.setStoredFilePath("evidence/confidential.png");
        image.setObjectKey("evidence/confidential.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        image.setStorageBackend("R2");
        SampleImages saved = sampleImagesRepository.save(image);

        // Switch security context to field_officer not on this sample
        UsernamePasswordAuthenticationToken otherAuth =
                new UsernamePasswordAuthenticationToken("field_officer", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ENVIRONMENTAL_FIELD_OFFICER")));
        SecurityContextHolder.getContext().setAuthentication(otherAuth);

        assertThrows(AccessDeniedException.class, () -> {
            evidenceStorageService.loadEvidenceAsResource(saved.getId());
        });
    }

    @Test
    @DisplayName("Resource-level authorization: Non-owner cannot delete another user's evidence")
    void shouldPreventNonOwnerFromDeletingEvidence() {
        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("owner_only.png");
        image.setStoredFilePath("evidence/owner_only.png");
        image.setObjectKey("evidence/owner_only.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        SampleImages saved = sampleImagesRepository.save(image);

        UsernamePasswordAuthenticationToken otherAuth =
                new UsernamePasswordAuthenticationToken("field_officer", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ENVIRONMENTAL_FIELD_OFFICER")));
        SecurityContextHolder.getContext().setAuthentication(otherAuth);

        assertThrows(AccessDeniedException.class, () -> {
            evidenceStorageService.deleteEvidence(saved.getId());
        });

        assertTrue(sampleImagesRepository.findById(saved.getId()).isPresent());
    }

    @Test
    @DisplayName("Safe two-phase deletion: R2 failure marks status PENDING_DELETION and preserves metadata")
    void shouldMarkPendingDeletionWhenR2Fails() {
        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("fail_delete.png");
        image.setStoredFilePath("evidence/fail_delete.png");
        image.setObjectKey("evidence/fail_delete.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        image.setStorageBackend("R2");
        SampleImages saved = sampleImagesRepository.save(image);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("Cloudflare R2 timeout"));

        assertThrows(ResponseStatusException.class, () -> {
            evidenceStorageService.deleteEvidence(saved.getId());
        });

        SampleImages updated = sampleImagesRepository.findById(saved.getId()).orElseThrow();
        assertEquals("PENDING_DELETION", updated.getStorageStatus());
    }

    @Test
    @DisplayName("Safe deletion: Repeated deletion request on PENDING_DELETION is idempotent and retries storage delete")
    void shouldRetryPendingDeletionIdempotently() {
        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("retry_delete.png");
        image.setStoredFilePath("evidence/retry_delete.png");
        image.setObjectKey("evidence/retry_delete.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setStorageStatus("PENDING_DELETION");
        image.setUploadedByUser(researcher);
        image.setStorageBackend("R2");
        SampleImages saved = sampleImagesRepository.save(image);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        evidenceStorageService.deleteEvidence(saved.getId());

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        assertFalse(sampleImagesRepository.findById(saved.getId()).isPresent());
    }
}
