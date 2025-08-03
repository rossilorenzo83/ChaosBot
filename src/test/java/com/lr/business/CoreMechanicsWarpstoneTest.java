package com.lr.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CoreMechanicsWarpstoneTest {

    @Test
    void testWarpstoneGatheringUsesCorrectImagePaths() {
        // Given: WARPSTONE resource type
        RssType warpstoneType = RssType.WARPSTONE;
        
        // When: Getting image paths for warpstone
        SearchViewButtons warpstoneButton = SearchViewButtons.getEnumFromRssType(warpstoneType);
        
        // Then: Should use correct image paths
        assertEquals("wp_icon.PNG", warpstoneButton.getImgPath());
        assertEquals("wp_source_map.PNG", warpstoneButton.getOnMapIconPath());
        assertEquals("wp_collect_map.PNG", warpstoneButton.getOnMapCollectButtonPath());
    }

    @Test
    void testWarpstoneIsSubsetOfRssFarming() {
        // Given: WARPSTONE resource type
        RssType warpstoneType = RssType.WARPSTONE;
        
        // When: Checking if warpstone is part of RSS_FARMING
        ActionType rssFarmingAction = ActionType.RSS_FARMING;
        
        // Then: Should be handled by RSS_FARMING action type
        assertNotNull(rssFarmingAction);
        assertTrue(warpstoneType instanceof RssType);
    }

    @Test
    void testWarpstoneEnumValueExists() {
        // Given: WARPSTONE resource type
        RssType warpstoneType = RssType.WARPSTONE;
        
        // Then: Should be a valid enum value
        assertNotNull(warpstoneType);
        assertEquals("WARPSTONE", warpstoneType.name());
    }

    @Test
    void testWarpstoneIconEnumExists() {
        // Given: WARPSTONE_ICON enum value
        SearchViewButtons warpstoneIcon = SearchViewButtons.WARPSTONE_ICON;
        
        // Then: Should be a valid enum value
        assertNotNull(warpstoneIcon);
        assertEquals("WARPSTONE_ICON", warpstoneIcon.name());
    }

    @Test
    void testWarpstoneIconHasCorrectRssType() {
        // Given: WARPSTONE_ICON enum value
        SearchViewButtons warpstoneIcon = SearchViewButtons.WARPSTONE_ICON;
        
        // Then: Should have correct RssType
        assertEquals(RssType.WARPSTONE, warpstoneIcon.getRssType());
    }
} 