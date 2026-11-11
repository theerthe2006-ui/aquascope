package com.aquascope.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class R2StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(R2StorageConfig.class);

    @Value("${app.r2.endpoint:}")
    private String r2Endpoint;

    @Value("${app.r2.access-key-id:}")
    private String accessKeyId;

    @Value("${app.r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${app.r2.region:auto}")
    private String region;

    @Bean
    public S3Client s3Client() {
        if (r2Endpoint == null || r2Endpoint.isBlank() || accessKeyId == null || accessKeyId.isBlank()) {
            log.info("Cloudflare R2 storage credentials not fully specified. Falling back to mock/null S3 client.");
            return null;
        }

        log.info("Initializing Cloudflare R2 S3 client with endpoint: {}", r2Endpoint);
        return S3Client.builder()
                .endpointOverride(URI.create(r2Endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .region(Region.of(region != null && !region.isBlank() ? region : "auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
