package com.aquascope.service;

import com.aquascope.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;

@Service
public class HealthService {

    private final DataSource dataSource;
    private final String appName;
    private final String appVersion;
    private final String activeProfile;

    public HealthService(
            DataSource dataSource,
            @Value("${spring.application.name:aquascope-backend}") String appName,
            @Value("${app.version:1.0.0}") String appVersion,
            @Value("${spring.profiles.active:default}") String activeProfile) {
        this.dataSource = dataSource;
        this.appName = appName;
        this.appVersion = appVersion;
        this.activeProfile = activeProfile;
    }

    public HealthResponse getSystemHealth() {
        boolean dbConnected = false;
        String dbProduct = "Unknown";
        String dbDetails = "Unavailable";

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            dbProduct = metaData.getDatabaseProductName();
            dbConnected = connection.isValid(2);
            dbDetails = dbProduct + " " + metaData.getDatabaseProductVersion();
        } catch (Exception e) {
            dbDetails = "Connection check failed: " + e.getMessage();
        }

        String overallStatus = dbConnected ? "UP" : "DEGRADED";

        return new HealthResponse(
                overallStatus,
                appName,
                appVersion,
                activeProfile,
                Instant.now(),
                new HealthResponse.DatabaseHealth(dbConnected, dbProduct, dbDetails)
        );
    }
}
