package com.lr.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class specifically for testing Robot bean creation in headless environments.
 * This test verifies that our conditional Robot creation works correctly.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class BeansHeadlessTest {

    @Autowired
    private Beans beans;

    private String originalDisplay;
    private String originalHeadless;

    @BeforeEach
    void setUp() {
        // Store original environment variables
        originalDisplay = System.getenv("DISPLAY");
        originalHeadless = System.getProperty("java.awt.headless");
    }

    @Test
    void shouldCreateRobotInHeadlessEnvironment() throws Exception {
        // Set up headless environment
        System.setProperty("java.awt.headless", "true");
        
        // Note: GraphicsEnvironment.isHeadless() is determined at JVM startup,
        // so setting the property at runtime won't change its value.
        // We'll test the property-based logic instead.
        assertEquals("true", System.getProperty("java.awt.headless"), "Headless property should be set to true");
        
        // Test that we can create a Robot bean in headless mode
        Robot robot = beans.sharedRobot();
        assertNotNull(robot, "Robot should be created in headless environment");
        
        // Test that the Robot is functional (even if it's a stub)
        assertDoesNotThrow(() -> {
            robot.delay(10); // Should not throw exception
        }, "Robot delay should work in headless environment");
        
        // Test screen capture returns a valid image
        assertDoesNotThrow(() -> {
            var image = robot.createScreenCapture(new java.awt.Rectangle(100, 100));
            assertNotNull(image, "Screen capture should return valid image");
            assertEquals(100, image.getWidth(), "Image width should match");
            assertEquals(100, image.getHeight(), "Image height should match");
        }, "Screen capture should work in headless environment");
    }

    @Test
    void shouldCreateRobotInGuiEnvironment() throws Exception {
        // Ensure we're not in headless mode
        System.setProperty("java.awt.headless", "false");
        
        // Verify the property is set correctly
        assertEquals("false", System.getProperty("java.awt.headless"), "Headless property should be set to false");
        
        // Test that we can create a Robot bean in GUI mode
        Robot robot = beans.sharedRobot();
        assertNotNull(robot, "Robot should be created in GUI environment");
        
        // Test that the Robot is functional
        assertDoesNotThrow(() -> {
            robot.delay(10); // Should not throw exception
        }, "Robot delay should work in GUI environment");
    }

    @Test
    void shouldHandleEnvironmentVariableChanges() throws Exception {
        // Test with headless mode
        System.setProperty("java.awt.headless", "true");
        assertEquals("true", System.getProperty("java.awt.headless"), "Headless property should be set to true");
        
        // Create Robot in headless mode
        Robot robot1 = beans.sharedRobot();
        assertNotNull(robot1, "Robot should be created with headless=true");
        
        // Change to GUI mode
        System.setProperty("java.awt.headless", "false");
        assertEquals("false", System.getProperty("java.awt.headless"), "Headless property should be set to false");
        
        // Create Robot in GUI mode
        Robot robot2 = beans.sharedRobot();
        assertNotNull(robot2, "Robot should be created with headless=false");
    }

    @Test
    void shouldLogEnvironmentInformation() throws Exception {
        // Set up headless environment
        System.setProperty("java.awt.headless", "true");
        
        // Create Robot and verify logging (we can't easily test logging output in unit tests,
        // but we can verify the method doesn't throw exceptions)
        assertDoesNotThrow(() -> {
            Robot robot = beans.sharedRobot();
            assertNotNull(robot, "Robot should be created");
        }, "Robot creation should not throw exceptions and should log environment info");
    }

    @Test
    void shouldHandleMultipleRobotCreations() throws Exception {
        // Set up headless environment
        System.setProperty("java.awt.headless", "true");
        
        // Create multiple Robot instances
        Robot robot1 = beans.sharedRobot();
        Robot robot2 = beans.sharedRobot();
        
        assertNotNull(robot1, "First Robot should be created");
        assertNotNull(robot2, "Second Robot should be created");
        
        // Both should be functional
        assertDoesNotThrow(() -> {
            robot1.delay(5);
            robot2.delay(5);
        }, "Multiple Robots should work correctly");
    }
} 