package com.lr.business;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SearchViewButtons enum methods.
 */
class SearchViewButtonsTest {

    @Test
    void getLevelIconImgPathShouldReturnAllLevelsForALL() {
        String result = SearchViewButtons.SEARCH_EXPANDER.getLevelIconImgPath("ALL", Locale.FRENCH);
        assertEquals("all_lvls_fr.PNG", result);
    }

    @Test
    void getLevelIconImgPathShouldReturnAllLevelsForALL_WO_EVENTS() {
        String result = SearchViewButtons.SEARCH_EXPANDER.getLevelIconImgPath("ALL_WO_EVENTS", Locale.FRENCH);
        assertEquals("all_lvls_fr.PNG", result, "ALL_WO_EVENTS should map to all_lvls icon");
    }

    @Test
    void getLevelIconImgPathShouldReturnAllLevelsForALL_WO_RELIC() {
        String result = SearchViewButtons.SEARCH_EXPANDER.getLevelIconImgPath("ALL_WO_RELIC", Locale.FRENCH);
        assertEquals("all_lvls_fr.PNG", result, "ALL_WO_RELIC should map to all_lvls icon");
    }

    @Test
    void getLevelIconImgPathShouldReturnSpecificLevelForNumber() {
        String result = SearchViewButtons.SEARCH_EXPANDER.getLevelIconImgPath("8", Locale.FRENCH);
        assertEquals("lvl_8.PNG", result);
    }

    @Test
    void getLevelIconImgPathShouldRespectLocale() {
        String frResult = SearchViewButtons.SEARCH_EXPANDER.getLevelIconImgPath("ALL", Locale.FRENCH);
        String enResult = SearchViewButtons.SEARCH_EXPANDER.getLevelIconImgPath("ALL", Locale.ENGLISH);

        assertEquals("all_lvls_fr.PNG", frResult);
        assertEquals("all_lvls_en.PNG", enResult);
    }
}
