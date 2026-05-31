package com.anipals.backend.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class RailwayDatabaseUrlProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String explicitSpringUrl = firstPresent(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("spring.datasource.url")
        );
        if (StringUtils.hasText(explicitSpringUrl) && !explicitSpringUrl.contains("localhost")) {
            return;
        }

        String databaseUrl = firstPresent(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("DATABASE_PUBLIC_URL")
        );

        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        try {
            URI uri = URI.create(databaseUrl);
            String userInfo = uri.getUserInfo();
            String[] credentials = userInfo == null ? new String[0] : userInfo.split(":", 2);
            String query = StringUtils.hasText(uri.getQuery()) ? "?" + uri.getQuery() : "";
            String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + uri.getPath() + query;

            properties.put("spring.datasource.url", jdbcUrl);
            if (credentials.length > 0 && !StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
                properties.put("spring.datasource.username", decode(credentials[0]));
            }
            if (credentials.length > 1 && !StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
                properties.put("spring.datasource.password", decode(credentials[1]));
            }
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Invalid DATABASE_URL for PostgreSQL: " + databaseUrl, ex);
        }

        environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrl", properties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String firstPresent(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
