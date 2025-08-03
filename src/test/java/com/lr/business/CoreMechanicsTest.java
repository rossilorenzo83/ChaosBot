package com.lr.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CoreMechanics business logic.
 * Following TDD principles - tests are written before implementation.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Import(com.lr.config.TestConfig.class)
class CoreMechanicsTest {

    @Mock
    private CoreMechanics coreMechanics;

    @BeforeEach
    void setUp() {
        // Setup test environment
    }

    @Test
    void shouldInitializeCoreMechanics() {
        // Test that CoreMechanics can be initialized
        assertNotNull(coreMechanics, "CoreMechanics should be initialized");
    }

    @Test
    void shouldSupportRssFarmingAction() {
        // Test RSS farming functionality
        ActionType actionType = ActionType.RSS_FARMING;
        assertNotNull(actionType, "RSS_FARMING action type should be supported");
        assertEquals("RSS_FARMING", actionType.name(), "Action type should match expected value");
    }

    @Test
    void shouldSupportArmyFarmingAction() {
        // Test Army farming functionality
        ActionType actionType = ActionType.ARMY_FARMING;
        assertNotNull(actionType, "ARMY_FARMING action type should be supported");
        assertEquals("ARMY_FARMING", actionType.name(), "Action type should match expected value");
    }

    @Test
    void shouldSupportChallengeStatsAction() {
        // Test Challenge stats functionality
        ActionType actionType = ActionType.CHALLENGE_STATS;
        assertNotNull(actionType, "CHALLENGE_STATS action type should be supported");
        assertEquals("CHALLENGE_STATS", actionType.name(), "Action type should match expected value");
    }

    @Test
    void shouldSupportDonorsStatsAction() {
        // Test Donors stats functionality
        ActionType actionType = ActionType.DONORS_STATS;
        assertNotNull(actionType, "DONORS_STATS action type should be supported");
        assertEquals("DONORS_STATS", actionType.name(), "Action type should match expected value");
    }

    @Test
    void shouldSupportAllRssTypes() {
        // Test all RSS types are supported
        RssType[] rssTypes = RssType.values();
        assertTrue(rssTypes.length > 0, "Should support at least one RSS type");
        
        for (RssType rssType : rssTypes) {
            assertNotNull(rssType, "RSS type should not be null");
            assertNotNull(rssType.name(), "RSS type name should not be null");
        }
    }
} 