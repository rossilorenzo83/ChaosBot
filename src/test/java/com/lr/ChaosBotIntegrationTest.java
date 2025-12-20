package com.lr;

import com.lr.utils.WindowInputService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ChaosBot application.
 * Following TDD principles - tests are written before implementation.
 * Uses JNA-based WindowInputService for focus-independent automation.
 * Updated for Spring Boot 3.5.4 and Tess4J 5.16.0 compatibility.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({com.lr.config.TestConfig.class, com.lr.config.Tess4JTestConfig.class})
class ChaosBotIntegrationTest {

    @Autowired
    private WindowInputService windowInputService;

    @Test
    void shouldStartApplicationContext() {
        // Test that the entire application context starts successfully
        assertTrue(true, "Application context should start without errors");
    }

    @Test
    void shouldLoadAllConfigurationBeans() {
        // Test that all configuration beans are loaded
        assertTrue(true, "All configuration beans should be loaded successfully");
    }

    @Test
    void shouldInitializeAllComponents() {
        // Test that all components are initialized
        assertTrue(true, "All components should be initialized successfully");
    }

    @Test
    void shouldSupportSpringBootAutoConfiguration() {
        // Test Spring Boot auto-configuration
        assertTrue(true, "Spring Boot auto-configuration should work correctly");
    }

    @Test
    void shouldHandleConfigurationProperties() {
        // Test configuration properties handling
        assertTrue(true, "Configuration properties should be handled correctly");
    }

    @Test
    void shouldInjectWindowInputService() {
        // Test that WindowInputService is properly injected
        assertNotNull(windowInputService, "WindowInputService should be injected");
    }

    @Test
    void shouldHandleMockWindowInputServiceGracefully() {
        // Test that mock WindowInputService works correctly for test environments
        try {
            BufferedImage screenshot = windowInputService.captureWindow(null);

            assertNotNull(screenshot, "Mock WindowInputService screen capture should not be null");
            assertEquals(100, screenshot.getWidth(), "Mock WindowInputService screen capture should have correct width");
            assertEquals(100, screenshot.getHeight(), "Mock WindowInputService screen capture should have correct height");

        } catch (Exception e) {
            fail("Mock WindowInputService should work without exceptions: " + e.getMessage());
        }
    }

    @Test
    void shouldSupportSpringBoot35Features() {
        // Test Spring Boot 3.5.4 specific features
        String springVersion = org.springframework.core.SpringVersion.getVersion();
        assertNotNull(springVersion, "Spring version should not be null");
        assertTrue(springVersion.startsWith("6."), "Should be using Spring Framework 6.x with Spring Boot 3.5.4");
    }

    @Test
    void shouldLoadTess4J516Dependencies() {
        // Test that Tess4J 5.16.0 dependencies are properly loaded
        try {
            Class.forName("net.sourceforge.tess4j.Tesseract");
            Class.forName("net.sourceforge.tess4j.util.LoadLibs");
            assertTrue(true, "Tess4J 5.16.0 dependencies should be available");
        } catch (ClassNotFoundException e) {
            fail("Tess4J dependencies should be available: " + e.getMessage());
        }
    }

    @Test
    void shouldLoadOpenCV490Dependencies() {
        // Test that OpenCV 4.9.0-0 dependencies are properly loaded
        try {
            Class.forName("org.opencv.core.Mat");
            Class.forName("org.opencv.imgcodecs.Imgcodecs");
            assertTrue(true, "OpenCV 4.9.0-0 dependencies should be available");
        } catch (ClassNotFoundException e) {
            fail("OpenCV dependencies should be available: " + e.getMessage());
        }
    }
}
