package com.myrtletrip.games.service;

import com.myrtletrip.MyrtleTripApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MyrtleTripApiApplicationTests {

    @Test
    void applicationEntryPointIsConfiguredAsSpringBootApplication() {
        assertTrue(
                MyrtleTripApiApplication.class.isAnnotationPresent(SpringBootApplication.class),
                "Application entry point must remain a Spring Boot application"
        );
    }
}
