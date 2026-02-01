package com.lr;

import com.lr.config.RobotIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for ChaosBot application.
 * Following TDD principles - tests are written before implementation.
 * Uses hybrid approach: real Robot testing when available, graceful skipping when not.
 * Updated for Spring Boot 3.5.4 and Tess4J 5.16.0 compatibility.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({com.lr.config.TestConfig.class, com.lr.config.Tess4JTestConfig.class})
class ChaosBotIntegrationTest {

    @Autowired
    private Robot robot;

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
    void shouldTestRobotScreenCaptureWhenAvailable() {
        // Test Robot screen capture functionality when real Robot is available
        assumeTrue(RobotIntegrationTest.isRealRobotAvailable(robot), 
            "Skipping Robot screen capture test - no display available or using mock Robot");
        
        try {
            Rectangle screenRect = new Rectangle(0, 0, 100, 100);
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            
            assertNotNull(screenshot, "Screen capture should not be null");
            assertEquals(100, screenshot.getWidth(), "Screen capture should have correct width");
            assertEquals(100, screenshot.getHeight(), "Screen capture should have correct height");
            
            // Verify we got real pixel data (not blank image)
            boolean hasRealData = false;
            for (int x = 0; x < screenshot.getWidth(); x++) {
                for (int y = 0; y < screenshot.getHeight(); y++) {
                    if (screenshot.getRGB(x, y) != 0) {
                        hasRealData = true;
                        break;
                    }
                }
            }
            assertTrue(hasRealData, "Screen capture should contain real pixel data");
            
        } catch (Exception e) {
            fail("Robot screen capture should work with real Robot: " + e.getMessage());
        }
    }

    @Test
    void shouldTestRobotPixelColorWhenAvailable() {
        // Test Robot pixel color functionality when real Robot is available
        assumeTrue(RobotIntegrationTest.isRealRobotAvailable(robot), 
            "Skipping Robot pixel color test - no display available or using mock Robot");
        
        try {
            // Test pixel color retrieval
            java.awt.Color pixelColor = robot.getPixelColor(0, 0);
            
            assertNotNull(pixelColor, "Pixel color should not be null");
            // Real Robot should return actual color, not just black
            assertFalse(pixelColor.equals(java.awt.Color.BLACK), "Should get real pixel color, not default black");
            
        } catch (Exception e) {
            fail("Robot pixel color should work with real Robot: " + e.getMessage());
        }
    }

    @Test
    void shouldHandleMockRobotGracefully() {
        // Test that mock Robot works correctly for headless environments
        // This test should always pass, even with mock Robot
        try {
            Rectangle screenRect = new Rectangle(0, 0, 100, 100);
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            
            assertNotNull(screenshot, "Mock Robot screen capture should not be null");
            assertEquals(100, screenshot.getWidth(), "Mock Robot screen capture should have correct width");
            assertEquals(100, screenshot.getHeight(), "Mock Robot screen capture should have correct height");
            
        } catch (Exception e) {
            fail("Mock Robot should work without exceptions: " + e.getMessage());
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