package com.myrtletrip;

import java.net.URI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Profiles;

@SpringBootApplication
public class MyrtleTripApiApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyrtleTripApiApplication.class);
        // An initializer runs after configuration is loaded but before Flyway / JPA
        // initialize. Fail closed if an E2E process targets the wrong database.
        app.addInitializers(context -> {
            var env = context.getEnvironment();
            if (!env.acceptsProfiles(Profiles.of("e2e"))) {
                return;
            }
            String jdbcUrl = env.getProperty("spring.datasource.url", "");
            final URI uri;
            try {
                if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
                    throw new IllegalArgumentException("Not a PostgreSQL JDBC URL");
                }
                uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            } catch (RuntimeException ex) {
                throw new IllegalStateException("E2E safety check: invalid JDBC URL", ex);
            }
            String host = uri.getHost();
            String path = uri.getPath();
            boolean localHost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
            boolean expectedDatabase = "/myrtle_trip_e2e_test".equals(path);
            boolean correctPort = uri.getPort() == 5432;
            if (!localHost || !expectedDatabase || !correctPort || uri.getRawQuery() != null
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
                throw new IllegalStateException(
                        "E2E safety check: datasource must be jdbc:postgresql://localhost:5432/myrtle_trip_e2e_test");
            }
            if (!"8081".equals(env.getProperty("server.port", ""))) {
                throw new IllegalStateException("E2E safety check: server.port must be 8081");
            }
        });
        app.run(args);
    }
}
