package com.myrtletrip.e2e;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Only available in the dedicated E2E profile. Never exposed in DEV or PROD. */
@Profile("e2e")
@RestController
public class E2eIdentityController {
    private final JdbcTemplate jdbcTemplate;

    public E2eIdentityController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/e2e/identity")
    public Map<String, String> identity() {
        String database = jdbcTemplate.queryForObject("select current_database()", String.class);
        if (!"myrtle_trip_e2e_test".equals(database)) {
            throw new IllegalStateException("E2E identity failed: unexpected database");
        }
        return Map.of("profile", "e2e", "database", database);
    }
}
