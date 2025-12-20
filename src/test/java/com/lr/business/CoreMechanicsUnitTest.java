package com.lr.business;

import com.lr.config.GeneralConfig;
import com.lr.utils.WindowInputService;
import com.lr.utils.WinUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CoreMechanics business logic.
 * Following TDD principles with proper mocking of dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CoreMechanicsUnitTest {

    @Mock
    private net.sourceforge.tess4j.Tesseract ocrEngine;

    @Mock
    private GeneralConfig generalConfig;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private WindowInputService windowInputService;

    @Mock
    private WinUtils.WindowInfo windowInfo;

    private CoreMechanics coreMechanics;

    @BeforeEach
    void setUp() throws Exception {
        coreMechanics = new CoreMechanics(ocrEngine, generalConfig, resourceLoader, windowInputService);

        // Initialize the mainMapButtonsCoordsMap since it's not initialized in constructor
        ConcurrentMap<String, java.util.Map<MainMapButtons, Double[]>> testMap = new ConcurrentHashMap<>();
        coreMechanics.setMainMapButtonsCoordsMap(testMap);
    }

    @Test
    void shouldInitializeCoreMechanicsWithDependencies() {
        // Test that CoreMechanics can be initialized with all dependencies
        assertNotNull(coreMechanics, "CoreMechanics should be initialized");
        assertNotNull(coreMechanics.getMainMapButtonsCoordsMap(), "Main map buttons coords map should be initialized");
    }

    @Test
    void shouldSetAndGetMainMapButtonsCoordsMap() {
        // Test the setter and getter for main map buttons coordinates
        ConcurrentMap<String, java.util.Map<MainMapButtons, Double[]>> testMap = new ConcurrentHashMap<>();
        coreMechanics.setMainMapButtonsCoordsMap(testMap);
        
        assertEquals(testMap, coreMechanics.getMainMapButtonsCoordsMap(), 
                    "Main map buttons coords map should be set and retrieved correctly");
    }

    @Test
    void shouldSupportAllActionTypes() {
        // Test that all action types are properly defined
        ActionType[] actionTypes = ActionType.values();
        assertTrue(actionTypes.length >= 4, "Should support at least 4 action types");
        
        for (ActionType actionType : actionTypes) {
            assertNotNull(actionType, "Action type should not be null");
            assertNotNull(actionType.name(), "Action type name should not be null");
            assertFalse(actionType.name().isEmpty(), "Action type name should not be empty");
        }
    }

    @Test
    void shouldSupportAllRssTypes() {
        // Test that all RSS types are properly defined
        RssType[] rssTypes = RssType.values();
        assertTrue(rssTypes.length >= 5, "Should support at least 5 RSS types");
        
        for (RssType rssType : rssTypes) {
            assertNotNull(rssType, "RSS type should not be null");
            assertNotNull(rssType.name(), "RSS type name should not be null");
            assertFalse(rssType.name().isEmpty(), "RSS type name should not be empty");
        }
    }

    @Test
    void shouldSupportAllMainMapButtons() {
        // Test that all main map buttons are properly defined
        MainMapButtons[] buttons = MainMapButtons.values();
        assertTrue(buttons.length > 0, "Should support at least one main map button");
        
        for (MainMapButtons button : buttons) {
            assertNotNull(button, "Main map button should not be null");
            assertNotNull(button.name(), "Main map button name should not be null");
            assertNotNull(button.getImgPath(), "Main map button image path should not be null");
        }
    }

    @Test
    void shouldSupportAllSearchViewButtons() {
        // Test that all search view buttons are properly defined
        SearchViewButtons[] buttons = SearchViewButtons.values();
        assertTrue(buttons.length > 0, "Should support at least one search view button");
        
        for (SearchViewButtons button : buttons) {
            assertNotNull(button, "Search view button should not be null");
            assertNotNull(button.name(), "Search view button name should not be null");
            assertNotNull(button.getImgPath(), "Search view button image path should not be null");
        }
    }

    @Test
    void shouldSupportAllExpeditionViewButtons() {
        // Test that all expedition view buttons are properly defined
        ExpeditionViewButtons[] buttons = ExpeditionViewButtons.values();
        assertTrue(buttons.length > 0, "Should support at least one expedition view button");
        
        for (ExpeditionViewButtons button : buttons) {
            assertNotNull(button, "Expedition view button should not be null");
            assertNotNull(button.name(), "Expedition view button name should not be null");
            assertNotNull(button.getImgPath(), "Expedition view button image path should not be null");
        }
    }

    @Test
    void shouldSupportAllChallengeViewButtons() {
        // Test that all challenge view buttons are properly defined
        ChallengeViewButtons[] buttons = ChallengeViewButtons.values();
        assertTrue(buttons.length > 0, "Should support at least one challenge view button");
        
        for (ChallengeViewButtons button : buttons) {
            assertNotNull(button, "Challenge view button should not be null");
            assertNotNull(button.name(), "Challenge view button name should not be null");
            assertNotNull(button.getImgPath(), "Challenge view button image path should not be null");
        }
    }

    @Test
    void shouldSupportAllReportViewButtons() {
        // Test that all report view buttons are properly defined
        ReportViewButtons[] buttons = ReportViewButtons.values();
        assertTrue(buttons.length > 0, "Should support at least one report view button");
        
        for (ReportViewButtons button : buttons) {
            assertNotNull(button, "Report view button should not be null");
            assertNotNull(button.name(), "Report view button name should not be null");
            assertNotNull(button.getImgPath(), "Report view button image path should not be null");
        }
    }

    @Test
    void shouldHandleImageNotMatchedException() {
        // Test that ImageNotMatchedException is properly defined
        ImageNotMatchedException exception = new ImageNotMatchedException("Test image not found", true);
        assertNotNull(exception, "ImageNotMatchedException should be created");
        assertEquals("Test image not found", exception.getMessage(), 
                    "Exception message should match the provided message");
        assertTrue(exception.getInMainMap(), "InMainMap should be set correctly");
    }

    @Test
    void shouldHaveCorrectConstants() {
        // Test that important constants are properly defined
        assertEquals(15, CoreMechanics.FAT_ARMY_THRESHOLD, 
                    "FAT_ARMY_THRESHOLD should be 15");
        assertEquals(20, CoreMechanics.SCROLL_AMOUNT, 
                    "SCROLL_AMOUNT should be 20");
        assertEquals(1, CoreMechanics.CONVERT_IMG_FLAG, 
                    "CONVERT_IMG_FLAG should be 1 (IMREAD_COLOR)");
    }

    @Test
    void shouldSupportSearchViewButtonsFromRssType() {
        // Test the mapping from RssType to SearchViewButtons
        for (RssType rssType : RssType.values()) {
            SearchViewButtons button = SearchViewButtons.getEnumFromRssType(rssType);
            assertNotNull(button, "SearchViewButtons should be found for RSS type: " + rssType);
            assertNotNull(button.getImgPath(), "Image path should not be null for RSS type: " + rssType);
        }
    }

    @Test
    void shouldHandleWindowCenterCoordinates() {
        // Test window center coordinate calculation
        WinUtils.RECT rect = new WinUtils.RECT();
        rect.left = 100;
        rect.top = 200;
        rect.right = 900;
        rect.bottom = 800;
        when(windowInfo.getRect()).thenReturn(rect);

        Double[] centerCoords = CoreMechanics.findWindowCenterCoords(windowInfo);
        
        assertNotNull(centerCoords, "Center coordinates should not be null");
        assertEquals(2, centerCoords.length, "Center coordinates should have 2 elements");
        assertEquals(500.0, centerCoords[0], 1.0, "X coordinate should be window center");
        assertEquals(500.0, centerCoords[1], 1.0, "Y coordinate should be window center");
    }

    @Test
    void shouldHandleWindowBottomCoordinates() {
        // Test window bottom coordinate calculation
        WinUtils.RECT rect = new WinUtils.RECT();
        rect.left = 100;
        rect.top = 200;
        rect.right = 900;
        rect.bottom = 800;
        when(windowInfo.getRect()).thenReturn(rect);

        Double[] bottomCoords = CoreMechanics.findWindowBottomCoords(windowInfo);
        
        assertNotNull(bottomCoords, "Bottom coordinates should not be null");
        assertEquals(2, bottomCoords.length, "Bottom coordinates should have 2 elements");
        assertEquals(500.0, bottomCoords[0], 1.0, "X coordinate should be window center");
        assertEquals(600.0, bottomCoords[1], 1.0, "Y coordinate should be window bottom");
    }
} 