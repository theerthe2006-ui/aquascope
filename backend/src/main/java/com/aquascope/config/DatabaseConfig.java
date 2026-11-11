package com.aquascope.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/aquascope}")
    private String rawDatasourceUrl;

    @Value("${spring.datasource.username:aquascope_user}")
    private String rawUsername;

    @Value("${spring.datasource.password:}")
    private String rawPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);

        // Neon Serverless PostgreSQL Hikari optimizations
        // Neon scales to zero when idle; avoid keeping dead idle connections and support smooth reconnect
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0); // Allow dropping all idle connections so Neon can sleep cleanly
        config.setIdleTimeout(60000); // 1 minute idle timeout before evicting connection
        config.setMaxLifetime(300000); // 5 minutes max lifetime to cycle connections
        config.setConnectionTimeout(30000); // 30s timeout for cold start / wake-up from scale-to-zero
        config.setValidationTimeout(5000); // 5s connection validation check
        config.setConnectionTestQuery("SELECT 1");

        String jdbcUrl = rawDatasourceUrl;
        String username = rawUsername;
        String password = rawPassword;

        // Parse postgres:// or postgresql:// (Neon / Render connection strings)
        if (rawDatasourceUrl != null && (rawDatasourceUrl.startsWith("postgres://") || rawDatasourceUrl.startsWith("postgresql://"))) {
            try {
                log.info("Parsing PostgreSQL URI format for JDBC compatibility");
                URI uri = new URI(rawDatasourceUrl.replace("postgresql://", "http://").replace("postgres://", "http://"));
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "aquascope";

                if (uri.getUserInfo() != null) {
                    String[] userParts = uri.getUserInfo().split(":", 2);
                    username = userParts[0];
                    if (userParts.length > 1) {
                        password = userParts[1];
                    }
                }

                String query = uri.getQuery();
                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                
                // Neon requires SSL
                if (query == null || !query.contains("sslmode=")) {
                    jdbcUrl += "?sslmode=require";
                } else {
                    jdbcUrl += "?" + query;
                }

                log.info("Neon PostgreSQL JDBC URL successfully configured for host: {}", host);
            } catch (Exception e) {
                log.error("Failed to parse PostgreSQL URI string, using raw URL", e);
            }
        } else if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql://") && !jdbcUrl.contains("sslmode=")) {
            // Ensure sslmode=require for Neon if not already specified
            if (jdbcUrl.contains("neon.tech")) {
                jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
            }
        }

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        return new HikariDataSource(config);
    }
}
