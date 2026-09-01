package com.college.cropadvisory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:${DB_URL:${SPRING_DATASOURCE_URL:${DATABASE_URL:jdbc:postgresql://localhost:5432/crop_advisory}}}}")
    private String rawUrl;

    @Value("${spring.datasource.username:${DB_USER:${SPRING_DATASOURCE_USERNAME:postgres}}}")
    private String rawUsername;

    @Value("${spring.datasource.password:${DB_PASSWORD:${SPRING_DATASOURCE_PASSWORD:password123}}}")
    private String rawPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String url = rawUrl;
        String username = rawUsername;
        String password = rawPassword;

        // Render / Heroku / Cloud environments provide postgres:// or postgresql:// URLs
        if (StringUtils.hasText(url) && (url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            try {
                String normalizedSchemeUrl = url.startsWith("postgres://")
                        ? url.replaceFirst("postgres://", "http://")
                        : url.replaceFirst("postgresql://", "http://");

                URI uri = new URI(normalizedSchemeUrl);

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (StringUtils.hasText(query)) {
                    jdbcUrl += "?" + query;
                }

                log.info("Normalized Cloud PostgreSQL URL to JDBC format for host: {}", host);
                url = jdbcUrl;

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if (userInfo.length > 0 && StringUtils.hasText(userInfo[0])) {
                        username = userInfo[0];
                    }
                    if (userInfo.length > 1 && StringUtils.hasText(userInfo[1])) {
                        password = userInfo[1];
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse database URI '{}', using as-is: {}", url, e.getMessage());
            }
        }

        config.setJdbcUrl(url);
        if (StringUtils.hasText(username)) {
            config.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            config.setPassword(password);
        }
        config.setDriverClassName("org.postgresql.Driver");

        // Optimized pool settings for cloud instances (Render free tier)
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1200000);

        return new HikariDataSource(config);
    }
}
