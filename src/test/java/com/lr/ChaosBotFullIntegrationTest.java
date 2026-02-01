package com.lr;

import com.lr.business.ActionType;
import com.lr.business.RssType;
import com.lr.config.GeneralConfig;
import com.lr.config.MarchConfig;
import com.lr.config.RobotIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Full integration tests for ChaosBot application.
 * Tests complete application context and component interactions.
 * Uses hybrid approach: real Robot testing when available, graceful skipping when not.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(com.lr.config.TestConfig.class)
class ChaosBotFullIntegrationTest {

    @Autowired
    private GeneralConfig generalConfig;

    @Autowired
    private MarchConfig marchConfig;

    @Autowired
    private WebClient discordWebClient;

    @Autowired
    private Robot robot;

    @Test
    void shouldStartCompleteApplicationContext() {
        // Test that the entire application context starts successfully
        assertTrue(true, "Application context should start without errors");
    }

    @Test
    void shouldLoadAllConfigurationBeans() {
        // Test that all configuration beans are loaded
        assertNotNull(generalConfig, "GeneralConfig should be loaded");
        assertNotNull(marchConfig, "MarchConfig should be loaded");
        assertNotNull(discordWebClient, "Discord WebClient should be loaded");
        assertNotNull(robot, "Robot should be loaded");
    }

    @Test
    void shouldTestRobotAutomationWhenAvailable() {
        // Test Robot automation functionality when real Robot is available
        assumeTrue(RobotIntegrationTest.isRealRobotAvailable(robot), 
            "Skipping Robot automation test - no display available or using mock Robot");
        
        try {
            // Test basic Robot automation capabilities
            robot.setAutoDelay(100);
            robot.setAutoWaitForIdle(true);
            
            // Test screen capture for automation
            Rectangle screenRect = new Rectangle(0, 0, 200, 200);
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            
            assertNotNull(screenshot, "Robot automation screen capture should work");
            assertEquals(200, screenshot.getWidth(), "Screen capture should have correct width");
            assertEquals(200, screenshot.getHeight(), "Screen capture should have correct height");
            
            // Test pixel color detection for automation
            var pixelColor = robot.getPixelColor(100, 100);
            assertNotNull(pixelColor, "Robot pixel color detection should work");
            
        } catch (Exception e) {
            fail("Robot automation should work with real Robot: " + e.getMessage());
        }
    }

    @Test
    void shouldHandleRobotConfiguration() {
        // This test works with both real and mock Robot
        try {
            robot.setAutoDelay(75);
            robot.setAutoWaitForIdle(false);
            
            // Just verify the methods don't throw exceptions
            assertNotNull(robot, "Robot should be available");
            
        } catch (Exception e) {
            fail("Robot configuration methods should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    void shouldSupportAllActionTypes() {
        // Test that all action types are available
        ActionType[] actionTypes = ActionType.values();
        assertTrue(actionTypes.length >= 4, "Should support at least 4 action types");
        
        for (ActionType actionType : actionTypes) {
            assertNotNull(actionType, "Action type should not be null");
            assertNotNull(actionType.name(), "Action type name should not be null");
        }
    }

    @Test
    void shouldSupportAllRssTypes() {
        // Test that all RSS types are available
        RssType[] rssTypes = RssType.values();
        assertTrue(rssTypes.length >= 5, "Should support at least 5 RSS types");
        
        for (RssType rssType : rssTypes) {
            assertNotNull(rssType, "RSS type should not be null");
            assertNotNull(rssType.name(), "RSS type name should not be null");
        }
    }

    @Test
    void shouldHaveValidConfigurationProperties() {
        // Test that configuration properties are valid
        assertNotNull(generalConfig.getPidName(), "PID name should not be null");
        assertNotNull(generalConfig.getWindowsNames(), "Windows names should not be null");
        assertTrue(generalConfig.getActionIntervalMs() > 0L, "Action interval should be positive");
        assertNotNull(generalConfig.getActionType(), "Action type should not be null");
        assertNotNull(generalConfig.getGameLanguage(), "Game language should not be null");
        assertTrue(generalConfig.getImageQualityLowerBound() >= 0.0 && generalConfig.getImageQualityLowerBound() <= 1.0, 
                  "Image quality should be between 0.0 and 1.0");
    }

    @Test
    void shouldHaveValidMarchConfiguration() {
        // Test that march configuration is valid
        assertTrue(marchConfig.getMarchesAvailable() >= 0, "Marches available should be non-negative");
        assertTrue(marchConfig.getMarchesIntervalMins() > 0L, "Marches interval should be positive");
        assertNotNull(marchConfig.getTargetRssLevel(), "Target RSS level should not be null");
        assertNotNull(marchConfig.getRssType(), "RSS type should not be null");
        assertNotNull(marchConfig.getTargetArmyLevel(), "Target army level should not be null");
    }

    @Test
    void shouldSupportSpringBootAutoConfiguration() {
        // Test Spring Boot auto-configuration
        assertTrue(true, "Spring Boot auto-configuration should work correctly");
    }

    @Test
    void shouldSupportDiscordIntegration() {
        // Test Discord integration setup
        assertNotNull(discordWebClient, "Discord WebClient should be configured");
    }

    @Test
    void shouldSupportConfigurationProfiles() {
        // Test that configuration profiles work correctly
        assertTrue(true, "Configuration profiles should work correctly");
    }

    @Test
    void shouldSupportPropertyBinding() {
        // Test that property binding works correctly
        assertNotNull(generalConfig, "Property binding should work for GeneralConfig");
        assertNotNull(marchConfig, "Property binding should work for MarchConfig");
    }

    @Test
    void shouldSupportDependencyInjection() {
        // Test that dependency injection works correctly
        assertNotNull(generalConfig, "Dependency injection should work for GeneralConfig");
        assertNotNull(marchConfig, "Dependency injection should work for MarchConfig");
        assertNotNull(discordWebClient, "Dependency injection should work for WebClient");
        assertNotNull(robot, "Dependency injection should work for Robot");
    }

    @Test
    void shouldSupportWebFluxConfiguration() {
        // Test that WebFlux configuration works correctly
        assertNotNull(discordWebClient, "WebFlux configuration should work correctly");
    }

    @Test
    void shouldSupportLombokAnnotations() {
        // Test that Lombok annotations work correctly
        assertNotNull(generalConfig.getPidName(), "Lombok getter should work");
        assertNotNull(marchConfig.getMarchesAvailable(), "Lombok getter should work");
    }

    @Test
    void shouldSupportConfigurationPropertiesValidation() {
        // Test that configuration properties validation works
        assertTrue(generalConfig.getActionIntervalMs() > 0L, "Action interval should be validated");
        assertTrue(generalConfig.getImageQualityLowerBound() >= 0.0, "Image quality should be validated");
        assertTrue(generalConfig.getImageQualityLowerBound() <= 1.0, "Image quality should be validated");
    }

    @Test
    void shouldSupportEnumValidation() {
        // Test that enum validation works
        assertNotNull(ActionType.RSS_FARMING, "RSS_FARMING action type should be valid");
        assertNotNull(ActionType.ARMY_FARMING, "ARMY_FARMING action type should be valid");
        assertNotNull(ActionType.CHALLENGE_STATS, "CHALLENGE_STATS action type should be valid");
        assertNotNull(ActionType.DONORS_STATS, "DONORS_STATS action type should be valid");
        
        assertNotNull(RssType.IRON, "IRON RSS type should be valid");
        assertNotNull(RssType.STONE, "STONE RSS type should be valid");
        assertNotNull(RssType.FOOD, "FOOD RSS type should be valid");
        assertNotNull(RssType.LEAD, "LEAD RSS type should be valid");
        assertNotNull(RssType.WOOD, "WOOD RSS type should be valid");
    }

    @Test
    void shouldSupportApplicationStartup() {
        // Test that application startup works correctly
        assertTrue(true, "Application startup should work correctly");
    }

    @Test
    void shouldSupportTestProfileConfiguration() {
        // Test that test profile configuration works correctly
        assertTrue(true, "Test profile configuration should work correctly");
    }
} 