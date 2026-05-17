package com.lr.business;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RssType enum and its helper methods.
 * These tests ensure the RSS type filtering for config options works correctly.
 */
class RssTypeTest {

    @Test
    void standardTypesShouldExcludeAllEventResources() {
        // Given
        RssType[] standardTypes = RssType.standardTypes();

        // Then
        Set<RssType> standardSet = Arrays.stream(standardTypes).collect(Collectors.toSet());

        // Should contain standard resources
        assertTrue(standardSet.contains(RssType.IRON), "Should contain IRON");
        assertTrue(standardSet.contains(RssType.STONE), "Should contain STONE");
        assertTrue(standardSet.contains(RssType.FOOD), "Should contain FOOD");
        assertTrue(standardSet.contains(RssType.LEAD), "Should contain LEAD");
        assertTrue(standardSet.contains(RssType.WOOD), "Should contain WOOD");

        // Should NOT contain event resources
        assertFalse(standardSet.contains(RssType.WARPSTONE), "Should NOT contain WARPSTONE");
        assertFalse(standardSet.contains(RssType.RELIC), "Should NOT contain RELIC");

        // Verify count
        assertEquals(5, standardTypes.length, "Should have exactly 5 standard types");
    }

    @Test
    void allExceptRelicShouldIncludeWarpstoneButExcludeRelic() {
        // Given
        RssType[] typesExceptRelic = RssType.allExceptRelic();

        // Then
        Set<RssType> typeSet = Arrays.stream(typesExceptRelic).collect(Collectors.toSet());

        // Should contain standard resources
        assertTrue(typeSet.contains(RssType.IRON), "Should contain IRON");
        assertTrue(typeSet.contains(RssType.STONE), "Should contain STONE");
        assertTrue(typeSet.contains(RssType.FOOD), "Should contain FOOD");
        assertTrue(typeSet.contains(RssType.LEAD), "Should contain LEAD");
        assertTrue(typeSet.contains(RssType.WOOD), "Should contain WOOD");

        // Should contain WARPSTONE
        assertTrue(typeSet.contains(RssType.WARPSTONE), "Should contain WARPSTONE");

        // Should NOT contain RELIC
        assertFalse(typeSet.contains(RssType.RELIC), "Should NOT contain RELIC");

        // Verify count (all 7 minus RELIC = 6)
        assertEquals(6, typesExceptRelic.length, "Should have exactly 6 types (all except RELIC)");
    }

    @Test
    void valuesShouldContainAllResourceTypes() {
        // Given
        RssType[] allTypes = RssType.values();

        // Then
        Set<RssType> allSet = Arrays.stream(allTypes).collect(Collectors.toSet());

        // Should contain all resources
        assertTrue(allSet.contains(RssType.IRON), "Should contain IRON");
        assertTrue(allSet.contains(RssType.STONE), "Should contain STONE");
        assertTrue(allSet.contains(RssType.FOOD), "Should contain FOOD");
        assertTrue(allSet.contains(RssType.LEAD), "Should contain LEAD");
        assertTrue(allSet.contains(RssType.WOOD), "Should contain WOOD");
        assertTrue(allSet.contains(RssType.WARPSTONE), "Should contain WARPSTONE");
        assertTrue(allSet.contains(RssType.RELIC), "Should contain RELIC");

        // Verify count
        assertEquals(7, allTypes.length, "Should have exactly 7 types total");
    }

    @Test
    void eventResourcesShouldBeMarkedAsEvents() {
        // Verify event flag is set correctly
        assertFalse(RssType.IRON.isEvent(), "IRON should not be an event resource");
        assertFalse(RssType.STONE.isEvent(), "STONE should not be an event resource");
        assertFalse(RssType.FOOD.isEvent(), "FOOD should not be an event resource");
        assertFalse(RssType.LEAD.isEvent(), "LEAD should not be an event resource");
        assertFalse(RssType.WOOD.isEvent(), "WOOD should not be an event resource");

        assertTrue(RssType.WARPSTONE.isEvent(), "WARPSTONE should be an event resource");
        assertTrue(RssType.RELIC.isEvent(), "RELIC should be an event resource");
    }

    @Test
    void standardTypesArrayShouldBeImmutableReference() {
        // Verify that calling standardTypes() multiple times returns the same array
        RssType[] first = RssType.standardTypes();
        RssType[] second = RssType.standardTypes();

        assertSame(first, second, "standardTypes() should return the same cached array");
    }

    @Test
    void allExceptRelicArrayShouldBeImmutableReference() {
        // Verify that calling allExceptRelic() multiple times returns the same array
        RssType[] first = RssType.allExceptRelic();
        RssType[] second = RssType.allExceptRelic();

        assertSame(first, second, "allExceptRelic() should return the same cached array");
    }
}
