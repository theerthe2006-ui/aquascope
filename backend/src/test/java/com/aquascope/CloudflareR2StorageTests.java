package com.aquascope;

import com.aquascope.dto.SampleImageDto;
import com.aquascope.entity.SampleImages;
import com.aquascope.entity.User;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.util.Optional;

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
                new UsernamePasswordAuthenticationToken("researcher", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should successfully upload image to Cloudflare R2 with signature check and SHA-256 checksum")
    void shouldUploadImageToR2WithChecksum() {
        // Valid PNG header (89 50 4E 47 0D 0A 1A 0A)
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
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
        assertNotNull(result.sha256Checksum());
        assertEquals(64, result.sha256Checksum().length()); // SHA-256 hex length
        assertEquals("image/png", result.contentType());

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(putCaptor.capture(), any(RequestBody.class));
        assertEquals(result.objectKey(), putCaptor.getValue().key());
    }

    @Test
    @DisplayName("Should reject file when signature does not match declared MIME type")
    void shouldRejectInvalidFileSignature() {
        // Declaring PNG but giving invalid random bytes
        byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        MockMultipartFile fakePng = new MockMultipartFile(
                "file", "fake.png", "image/png", invalidBytes
        );

        assertThrows(IllegalArgumentException.class, () -> {
            evidenceStorageService.storeEvidence(fakePng, null, null, null, null, null);
        });

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should stream stored object from R2 via authenticated backend endpoint")
    void shouldRetrieveAndStreamObjectFromR2() {
        byte[] sampleData = "scientific-binary-content".getBytes();
        GetObjectResponse getResponse = GetObjectResponse.builder().build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(getResponse, sampleData);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        // Create test entity in repository
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
    @DisplayName("Should safely delete evidence from R2 and repository")
    void shouldDeleteEvidenceFromR2() {
        User researcher = userRepository.findByUsername("researcher").orElseThrow();
        SampleImages image = new SampleImages();
        image.setFileName("to_delete.png");
        image.setStoredFilePath("evidence/to_delete.png");
        image.setObjectKey("evidence/to_delete.png");
        image.setContentType("image/png");
        image.setFileSizeBytes(100L);
        image.setUploadedByUser(researcher);
        SampleImages saved = sampleImagesRepository.save(image);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        evidenceStorageService.deleteEvidence(saved.getId());

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        assertFalse(sampleImagesRepository.findById(saved.getId()).isPresent());
    }
}
