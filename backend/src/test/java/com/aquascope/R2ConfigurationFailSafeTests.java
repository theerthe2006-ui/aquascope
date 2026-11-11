package com.aquascope;

import com.aquascope.config.R2StorageConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;

public class R2ConfigurationFailSafeTests {

    @Test
    @DisplayName("Production profile MUST throw IllegalStateException if R2 credentials are missing")
    void shouldFailStartupInProductionIfCredentialsMissing() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        R2StorageConfig config = new R2StorageConfig(env);
        ReflectionTestUtils.setField(config, "r2Endpoint", "");
        ReflectionTestUtils.setField(config, "r2Bucket", "");
        ReflectionTestUtils.setField(config, "accessKeyId", "");
        ReflectionTestUtils.setField(config, "secretAccessKey", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class, config::s3Client);
        assertTrue(exception.getMessage().contains("Production startup failed: Cloudflare R2 storage credentials must be fully configured"));
    }

    @Test
    @DisplayName("Non-production profile falls back to null S3Client with local fallback when credentials missing")
    void shouldFallbackInDevProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        R2StorageConfig config = new R2StorageConfig(env);
        ReflectionTestUtils.setField(config, "r2Endpoint", "");
        ReflectionTestUtils.setField(config, "r2Bucket", "");
        ReflectionTestUtils.setField(config, "accessKeyId", "");
        ReflectionTestUtils.setField(config, "secretAccessKey", "");

        S3Client client = config.s3Client();
        assertNull(client, "S3Client should be null in dev profile when credentials are blank");
    }
}
