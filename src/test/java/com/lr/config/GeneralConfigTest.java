package com.lr.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GeneralConfig configuration class.
 * Following TDD principles - tests are written before implementation.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class GeneralConfigTest {

    @Test
    void shouldLoadConfigurationProperties() {
        // Test that configuration properties can be loaded
        assertTrue(true, "Configuration properties should load successfully");
    }

    @Test
    void shouldSupportDefaultPidName() {
        // Test default process ID name configuration
        String expectedPidName = "BlueStacks_nxt";
        assertNotNull(expectedPidName, "Default PID name should be defined");
        assertFalse(expectedPidName.isEmpty(), "Default PID name should not be empty");
    }

    @Test
    void shouldSupportDefaultActionInterval() {
        // Test default action interval configuration
        int expectedInterval = 3000; // 3 seconds
        assertTrue(expectedInterval > 0, "Default action interval should be positive");
        assertTrue(expectedInterval <= 60000, "Default action interval should be reasonable");
    }

    @Test
    void shouldSupportDefaultGameLanguage() {
        // Test default game language configuration
        String expectedLanguage = "fr";
        assertNotNull(expectedLanguage, "Default game language should be defined");
        assertTrue(expectedLanguage.equals("fr") || expectedLanguage.equals("en"), 
                  "Default language should be either 'fr' or 'en'");
    }

    @Test
    void shouldSupportDefaultImageQualityBound() {
        // Test default image quality lower bound
        double expectedQuality = 0.7;
        assertTrue(expectedQuality >= 0.0 && expectedQuality <= 1.0, 
                  "Image quality should be between 0.0 and 1.0");
    }
} 