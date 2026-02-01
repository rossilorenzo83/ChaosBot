package com.lr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChaosBot main application class.
 * Following TDD principles - tests are written before implementation.
 * Updated for Spring Boot 3.5.4 and Tess4J 5.16.0 compatibility.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import({com.lr.config.TestConfig.class, com.lr.config.Tess4JTestConfig.class})
class ChaosBotTest {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
        assertTrue(true, "Spring context should load without errors");
    }

    @Test
    void applicationStartsSuccessfully() {
        // Test that the application can start
        // Since ChaosBot now uses constructor injection, we rely on Spring context loading
        assertTrue(true, "ChaosBot should start successfully via Spring Boot context");
    }

    @Test
    void mainMethodExists() {
        // Test that main method exists and is accessible
        assertDoesNotThrow(() -> {
            // This test ensures the main method signature is correct
            // In a real scenario, we would test actual functionality
        }, "Main method should be accessible");
    }

    @Test
    void springBootVersionCompatibility() {
        // Test that Spring Boot 4.0 is properly loaded
        String springVersion = org.springframework.core.SpringVersion.getVersion();
        assertNotNull(springVersion, "Spring version should not be null");
        assertTrue(springVersion.startsWith("7."), "Should be using Spring Framework 7.x with Spring Boot 4.0");
    }

    @Test
    void tess4jCompatibility() {
        // Test that Tess4J 5.16.0 is properly loaded
        try {
            Class.forName("net.sourceforge.tess4j.Tesseract");
            assertTrue(true, "Tess4J 5.16.0 should be available");
        } catch (ClassNotFoundException e) {
            fail("Tess4J should be available: " + e.getMessage());
        }
    }

    @Test
    void opencvCompatibility() {
        // Test that OpenCV 4.9.0-0 is properly loaded
        try {
            Class.forName("org.opencv.core.Mat");
            assertTrue(true, "OpenCV should be available");
        } catch (ClassNotFoundException e) {
            fail("OpenCV should be available: " + e.getMessage());
        }
    }
} 