package com.aquascope;

import com.aquascope.dto.SampleImageDto;
import com.aquascope.entity.SampleImages;
import com.aquascope.entity.User;
import com.aquascope.exception.ResourceNotFoundException;
import com.aquascope.repository.SampleImagesRepository;
import com.aquascope.repository.UserRepository;
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

import java.util.Collections;
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

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("researcher", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_LABORATORY_RESEARCHER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should successfully upload PNG image to Cloudflare R2 with signature check and SHA-256 checksum")
    void shouldUploadImageToR2WithChecksum() {
        // Valid PNG header (89 50 4E 47 0D 0A 1A 0A) followed by 8 bytes
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52};
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

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(putCaptor.capture(), any(RequestBody.class));
        assertEquals(result.objectKey(), putCaptor.getValue().key());
    }

    @Test
    @DisplayName("Should successfully upload WebP image when valid RIFF...WEBP header is provided")
    void shouldUploadWebPImage() {
        // Valid WebP header: RIFF (bytes 0-3) + file size (bytes 4-7) + WEBP (bytes 8-11)
        byte[] webpBytes = new byte[]{
                'R', 'I', 'F', 'F',
                0x20, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P',
                'V', 'P', '8', ' '
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "fiber.webp", "image/webp", webpBytes
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        SampleImageDto result = evidenceStorageService.storeEvidence(
                file, null, null, "BRIGHTFIELD_MICROSCOPY", "100x", "Microfiber WebP"
        );

        assertNotNull(result);
        assertEquals("image/webp", result.contentType());
        assertTrue(result.objectKey().endsWith(".webp"));
    }

    @Test
    @DisplayName("Should reject file when signature does not match supported MIME signatures")
    void shouldRejectInvalidFileSignature() {
        byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C};
        MockMultipartFile fakePng = new MockMultipartFile(
                "file", "fake.png", "image/png", invalidBytes
        );

        assertThrows(IllegalArgumentException.class, () -> {
            evidenceStorageService.storeEvidence(fakePng, null, null, null, null, null);
        });

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should reject invalid WebP signature missing WEBP magic marker")
    void shouldRejectCorruptWebPSignature() {
        // Starts with RIFF but has invalid 8-11 bytes
        byte[] corruptWebp = new byte[]{
                'R', 'I', 'F', 'F',
                0x20, 0x00, 0x00, 0x00,
                'B', 'A', 'D', '!',
                '0', '0', '0', '0'
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupt.webp", "image/webp", corruptWebp
        );

        assertThrows(IllegalArgumentException.class, () -> {
            evidenceStorageService.storeEvidence(file, null, null, null, null, null);
        });
    }

    @Test
    @DisplayName("Should reject oversized image")
    void shouldRejectOversizedImage() {
        // Set up mock with large length without allocating 25MB buffer
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
        SampleImages saved = sampleImagesRepository.save(image);

        Resource resource = evidenceStorageService.loadEvidenceAsResource(saved.getId());
        assertNotNull(resource);
        assertTrue(resource.exists());

        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should return 404 ResourceNotFoundException when R2 object does not exist (NoSuchKeyException)")
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
        SampleImages saved = sampleImagesRepository.save(image);

        assertThrows(ResourceNotFoundException.class, () -> {
            evidenceStorageService.loadEvidenceAsResource(saved.getId());
        });
    }

    @Test
    @DisplayName("Should return 503 ResponseStatusException when temporary R2 outage or network error occurs")
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
        SampleImages saved = sampleImagesRepository.save(image);

        assertThrows(ResponseStatusException.class, () -> {
            evidenceStorageService.loadEvidenceAsResource(saved.getId());
        });
    }

    @Test
    @DisplayName("Should deny access when user has unprivileged role")
    void shouldDenyUnauthorizedUserEvidenceAccess() {
        UsernamePasswordAuthenticationToken unprivilegedAuth =
                new UsernamePasswordAuthenticationToken("guest", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        SecurityContextHolder.getContext().setAuthentication(unprivilegedAuth);

        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("private.png");
        image.setStoredFilePath("evidence/private.png");
        image.setObjectKey("evidence/private.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        SampleImages saved = sampleImagesRepository.save(image);

        assertThrows(AccessDeniedException.class, () -> {
            evidenceStorageService.loadEvidenceAsResource(saved.getId());
        });
    }

    @Test
    @DisplayName("Should preserve database metadata if R2 object deletion fails")
    void shouldPreserveDatabaseMetadataWhenR2DeleteFails() {
        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("protected_meta.png");
        image.setStoredFilePath("evidence/protected_meta.png");
        image.setObjectKey("evidence/protected_meta.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        SampleImages saved = sampleImagesRepository.save(image);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("R2 S3 connection timeout"));

        assertThrows(ResponseStatusException.class, () -> {
            evidenceStorageService.deleteEvidence(saved.getId());
        });

        // Verify entity is still present in database
        assertTrue(sampleImagesRepository.findById(saved.getId()).isPresent());
    }
}
