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
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);

        String jdbcUrl = rawDatasourceUrl;
        String username = rawUsername;
        String password = rawPassword;

        // Render provides DATABASE_URL or connectionString in format: postgres://user:password@host:port/dbname
        // or postgresql://user:password@host:port/dbname
        if (rawDatasourceUrl != null && (rawDatasourceUrl.startsWith("postgres://") || rawDatasourceUrl.startsWith("postgresql://"))) {
            try {
                log.info("Converting standard PostgreSQL URI to JDBC URL for Spring Boot compatibility");
                URI uri = new URI(rawDatasourceUrl.replace("postgresql://", "http://").replace("postgres://", "http://"));
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath(); // /dbname
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
                if (query != null && !query.isEmpty()) {
                    jdbcUrl += "?" + query;
                }
                log.info("JDBC URL successfully mapped to: jdbc:postgresql://{}:{}/{}", host, port, dbName);
            } catch (Exception e) {
                log.error("Failed to parse postgres URL format; falling back to direct string", e);
            }
        }

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        return new HikariDataSource(config);
    }
}
