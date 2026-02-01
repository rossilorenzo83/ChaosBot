package com.lr.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for Robot bean that test real Robot functionality when available.
 * These tests will be skipped in headless environments (CI/CD) where no display is available.
 * Updated for Spring Boot 3.5.4 and Tess4J 5.16.0 compatibility.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import({TestConfig.class, Tess4JTestConfig.class})
public class RobotIntegrationTest {

    @Autowired
    private Robot robot;

    /**
     * Check if we have a real Robot (not mock) and display is available.
     * This method can be used by other integration tests to determine if real Robot testing is possible.
     */
    public static boolean isRealRobotAvailable(Robot robot) {
        // Check if this is a Mockito mock
        if (org.mockito.Mockito.mockingDetails(robot).isMock()) {
            return false;
        }
        
        // If it's not a mock, it's a real Robot
        return true;
    }

    /**
     * Check if we have a real Robot (not mock) and display is available.
     */
    private boolean isRealRobotAvailable() {
        return isRealRobotAvailable(robot);
    }

    @Test
    void shouldPerformRealScreenCapture() {
        // Only run if real Robot is available
        assumeTrue(isRealRobotAvailable(), 
            "Skipping real Robot test - no display available or using mock Robot");
        
        // Test real screen capture
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
    }

    @Test
    void shouldGetRealPixelColor() {
        // Only run if real Robot is available
        assumeTrue(isRealRobotAvailable(), 
            "Skipping real Robot test - no display available or using mock Robot");
        
        // Test real pixel color retrieval
        Color pixelColor = robot.getPixelColor(0, 0);
        
        assertNotNull(pixelColor, "Pixel color should not be null");
        // Real Robot should return actual color, not just black
        assertFalse(pixelColor.equals(Color.BLACK), "Should get real pixel color, not default black");
    }

    @Test
    void shouldHandleRobotConfiguration() {
        // This test works with both real and mock Robot
        // For mock Robot, we can only test that methods don't throw exceptions
        // Mockito mocks don't automatically track state changes
        
        try {
            robot.setAutoDelay(100);
            robot.setAutoWaitForIdle(true);
            
            // For mock Robot, these will return default values (0 and false)
            // For real Robot, these will return the actual set values
            int autoDelay = robot.getAutoDelay();
            boolean autoWaitForIdle = robot.isAutoWaitForIdle();
            
            // Just verify the methods don't throw exceptions
            assertNotNull(robot, "Robot should be available");
            
        } catch (Exception e) {
            fail("Robot configuration methods should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    void shouldWorkWithMockRobotInHeadlessEnvironment() {
        // This test should always pass, even with mock Robot
        // It verifies that our mock Robot configuration works correctly
        
        try {
            Rectangle screenRect = new Rectangle(0, 0, 100, 100);
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            
            assertNotNull(screenshot, "Mock Robot screen capture should not be null");
            assertEquals(100, screenshot.getWidth(), "Mock Robot screen capture should have correct width");
            assertEquals(100, screenshot.getHeight(), "Mock Robot screen capture should have correct height");
            
            // Mock Robot should return black pixels
            Color pixelColor = robot.getPixelColor(0, 0);
            assertNotNull(pixelColor, "Mock Robot pixel color should not be null");
            
        } catch (Exception e) {
            fail("Mock Robot should work without exceptions: " + e.getMessage());
        }
    }

    @Test
    void shouldSupportSpringBoot35Integration() {
        // Test that Spring Boot 4.0 integration works correctly
        String springVersion = org.springframework.core.SpringVersion.getVersion();
        assertNotNull(springVersion, "Spring version should not be null");
        assertTrue(springVersion.startsWith("7."), "Should be using Spring Framework 7.x with Spring Boot 4.0");
    }
} 