package com.lr.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * Test configuration that provides a conditional Robot bean for testing.
 * This allows tests to run with real Robot when display is available, or mock Robot when not.
 */
@TestConfiguration
public class TestConfig {

    private final Environment environment;

    public TestConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Provides a conditional Robot bean for tests.
     * - Real Robot when display is available and not in CI/CD
     * - Mock Robot when no display is available (headless environments)
     */
    @Bean
    public Robot robot() {
        // Check if we're in a headless environment
        String display = System.getenv("DISPLAY");
        boolean isHeadless = GraphicsEnvironment.isHeadless();
        String osName = System.getProperty("os.name", "").toLowerCase();
        
        // On Windows, we can create a real Robot if we have a GUI
        // On Linux/Unix, we need DISPLAY variable
        boolean canCreateRealRobot = false;
        
        if (osName.contains("windows")) {
            // Windows: check if we have a GUI (not headless)
            canCreateRealRobot = !isHeadless;
        } else {
            // Linux/Unix: check if DISPLAY is available
            canCreateRealRobot = display != null && !display.isEmpty() && !isHeadless;
        }
        
        // If we can create a real Robot, try to do so
        if (canCreateRealRobot) {
            try {
                System.out.println("Creating REAL Robot for tests - GUI available");
                return new Robot();
            } catch (AWTException e) {
                System.out.println("Failed to create real Robot: " + e.getMessage() + " - falling back to mock");
                // Fall back to mock Robot
            }
        }
        
        // Create a Mockito mock of Robot - no display required
        System.out.println("Creating MOCK Robot for tests - no GUI available");
        Robot mockRobot = mock(Robot.class);
        
        // Configure the mock to return safe values for screen capture
        when(mockRobot.createScreenCapture(any(Rectangle.class)))
            .thenReturn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));
        
        when(mockRobot.getPixelColor(anyInt(), anyInt()))
            .thenReturn(Color.BLACK);
        
        // Configure auto delay methods to work with any input
        when(mockRobot.getAutoDelay()).thenReturn(0);
        when(mockRobot.isAutoWaitForIdle()).thenReturn(false);
        
        // Allow setAutoDelay and setAutoWaitForIdle to be called without throwing exceptions
        // Note: Mockito mocks don't automatically track state, so we return default values
        
        // Return the configured mock
        return mockRobot;
    }
} 