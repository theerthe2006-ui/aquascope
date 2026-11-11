package com.aquascope.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.Arrays;

@Configuration
public class R2StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(R2StorageConfig.class);

    private final Environment env;

    @Value("${app.r2.endpoint:}")
    private String r2Endpoint;

    @Value("${app.r2.bucket:}")
    private String r2Bucket;

    @Value("${app.r2.access-key-id:}")
    private String accessKeyId;

    @Value("${app.r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${app.r2.region:auto}")
    private String region;

    public R2StorageConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public S3Client s3Client() {
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");

        boolean hasEndpoint = r2Endpoint != null && !r2Endpoint.isBlank();
        boolean hasBucket = r2Bucket != null && !r2Bucket.isBlank();
        boolean hasAccessKey = accessKeyId != null && !accessKeyId.isBlank();
        boolean hasSecretKey = secretAccessKey != null && !secretAccessKey.isBlank();

        if (isProd) {
            if (!hasEndpoint || !hasBucket || !hasAccessKey || !hasSecretKey) {
                String missing = String.format("Missing required R2 configuration in 'prod' profile. endpoint=%b, bucket=%b, accessKey=%b, secretKey=%b",
                        hasEndpoint, hasBucket, hasAccessKey, hasSecretKey);
                log.error("FATAL: {}", missing);
                throw new IllegalStateException("Production startup failed: Cloudflare R2 storage credentials must be fully configured. " + missing);
            }
        }

        if (!hasEndpoint || !hasAccessKey || !hasSecretKey) {
            log.info("Cloudflare R2 storage credentials not fully specified in non-production profile. Local filesystem fallback enabled.");
            return null;
        }

        log.info("Initializing Cloudflare R2 S3 client with endpoint: {}, bucket: {}", r2Endpoint, r2Bucket);
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
