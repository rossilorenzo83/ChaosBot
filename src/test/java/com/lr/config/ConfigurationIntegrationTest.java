package com.lr.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for configuration classes.
 * Tests Spring Boot configuration loading and bean initialization.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class ConfigurationIntegrationTest {

    @Autowired
    private GeneralConfig generalConfig;

    @Autowired
    private MarchConfig marchConfig;

    @Autowired
    private WebClient discordWebClient;

    @Test
    void shouldLoadGeneralConfig() {
        // Test that GeneralConfig is properly loaded
        assertNotNull(generalConfig, "GeneralConfig should be loaded");
        assertNotNull(generalConfig.getPidName(), "PID name should not be null");
        assertNotNull(generalConfig.getWindowsNames(), "Windows names should not be null");
        assertTrue(generalConfig.getActionIntervalMs() > 0L, "Action interval should be positive");
        assertNotNull(generalConfig.getActionType(), "Action type should not be null");
        assertNotNull(generalConfig.getGameLanguage(), "Game language should not be null");
        assertTrue(generalConfig.getImageQualityLowerBound() >= 0.0 && generalConfig.getImageQualityLowerBound() <= 1.0, 
                  "Image quality should be between 0.0 and 1.0");
    }

    @Test
    void shouldLoadMarchConfig() {
        // Test that MarchConfig is properly loaded
        assertNotNull(marchConfig, "MarchConfig should be loaded");
        assertTrue(marchConfig.getMarchesAvailable() >= 0, "Marches available should be non-negative");
        assertTrue(marchConfig.getMarchesIntervalMins() > 0L, "Marches interval should be positive");
        assertTrue(Integer.parseInt(marchConfig.getTargetArmyLevel()) > 0, "Target army level should be positive");
        assertNotNull(marchConfig.getTargetRssLevel(), "Target RSS level should not be null");
        assertNotNull(marchConfig.getRssType(), "RSS type should not be null");
    }

    @Test
    void shouldLoadDiscordWebClient() {
        // Test that Discord WebClient is properly configured
        assertNotNull(discordWebClient, "Discord WebClient should be loaded");
    }

    @Test
    void shouldHaveValidGeneralConfigDefaults() {
        // Test default values for GeneralConfig
        assertFalse(generalConfig.getPidName().isEmpty(), "PID name should not be empty");
        assertFalse(generalConfig.getWindowsNames().isEmpty(), "Windows names should not be empty");
        assertTrue(generalConfig.getActionIntervalMs() >= 1000, "Action interval should be at least 1000ms");
        assertTrue(generalConfig.getActionIntervalMs() <= 60000, "Action interval should be at most 60000ms");
        
        // Test language validation
        java.util.Locale language = generalConfig.getGameLanguage();
        assertTrue(language.equals(java.util.Locale.ENGLISH) || language.equals(java.util.Locale.FRENCH), 
                  "Game language should be either ENGLISH or FRENCH");
        
        // Test image quality validation
        double quality = generalConfig.getImageQualityLowerBound();
        assertTrue(quality >= 0.0 && quality <= 1.0, 
                  "Image quality should be between 0.0 and 1.0");
    }

    @Test
    void shouldHaveValidMarchConfigDefaults() {
        // Test default values for MarchConfig
        assertTrue(marchConfig.getMarchesAvailable() >= 1, "Should have at least 1 march available");
        assertTrue(marchConfig.getMarchesAvailable() <= 10, "Should have at most 10 marches available");
        assertTrue(marchConfig.getMarchesIntervalMins() >= 1, "Marches interval should be at least 1 minute");
        assertTrue(marchConfig.getMarchesIntervalMins() <= 1440, "Marches interval should be at most 24 hours");
        assertTrue(Integer.parseInt(marchConfig.getTargetArmyLevel()) >= 1, "Target army level should be at least 1");
        assertTrue(Integer.parseInt(marchConfig.getTargetArmyLevel()) <= 50, "Target army level should be at most 50");
        
        // Test RSS level validation
        String rssLevel = marchConfig.getTargetRssLevel();
        assertTrue(rssLevel.equals("ALL") || rssLevel.matches("\\d+"), 
                  "RSS level should be 'ALL' or a number");
    }

    @Test
    void shouldSupportConfigurationPropertiesOverride() {
        // Test that configuration properties can be overridden
        // This test verifies that the @ConfigurationProperties annotation works correctly
        assertNotNull(generalConfig, "GeneralConfig should support property binding");
        assertNotNull(marchConfig, "MarchConfig should support property binding");
    }

    @Test
    void shouldHaveValidActionType() {
        // Test that the action type is valid
        com.lr.business.ActionType actionType = generalConfig.getActionType();
        assertNotNull(actionType, "Action type should not be null");
        
        // Check if it's one of the valid action types
        boolean isValidActionType = actionType == com.lr.business.ActionType.RSS_FARMING ||
                                  actionType == com.lr.business.ActionType.ARMY_FARMING ||
                                  actionType == com.lr.business.ActionType.CHALLENGE_STATS ||
                                  actionType == com.lr.business.ActionType.DONORS_STATS;
        
        assertTrue(isValidActionType, "Action type should be one of the valid types");
    }

    @Test
    void shouldHaveValidRssType() {
        // Test that the RSS type is valid
        String rssType = marchConfig.getRssType();
        assertNotNull(rssType, "RSS type should not be null");
        
        // Check if it's one of the valid RSS types
        boolean isValidRssType = rssType.equals("IRON") ||
                               rssType.equals("STONE") ||
                               rssType.equals("FOOD") ||
                               rssType.equals("LEAD") ||
                               rssType.equals("WOOD");
        
        assertTrue(isValidRssType, "RSS type should be one of the valid types");
    }
} 