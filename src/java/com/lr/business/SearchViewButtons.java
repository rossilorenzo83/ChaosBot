package com.lr.business;

import java.util.Locale;
import java.util.Optional;

import static com.lr.config.Config.GAME_LANGUAGE;

public enum SearchViewButtons {

    SEARCH_EXPANDER("search_type_expander.PNG", Optional.empty(), Optional.empty(), Optional.empty()),
    SEARCH_MAP(Locale.FRENCH.equals(GAME_LANGUAGE)? "search_button_fr.PNG":"", Optional.empty(), Optional.empty(), Optional.empty()),
    GO_RSS(Locale.FRENCH.equals(GAME_LANGUAGE)? "go_rss_fr.PNG":"go_rss_agnostic.PNG", Optional.empty(), Optional.empty(), Optional.empty()),
    STONE_ICON("stone_icon.PNG", Optional.of(RssType.STONE), Optional.of("stone_source_map.PNG"), Optional.of("stone_collect_map.PNG")),
    LEAD_ICON("lead_icon.PNG", Optional.of(RssType.LEAD), Optional.of("lead_source_map.PNG"), Optional.of("lead_collect_map.PNG")),
    FOOD_ICON("farm_icon.PNG", Optional.of(RssType.FOOD), Optional.of("food_source_map.PNG"), Optional.of("food_collect_map.PNG")),
    IRON_ICON("iron_icon.PNG", Optional.of(RssType.IRON), Optional.of("iron_source_map.PNG"), Optional.of("iron_collect_map.PNG")),
    WOOD_ICON("wood_icon.PNG", Optional.of(RssType.WOOD), Optional.of("wood_source_map.PNG"), Optional.of("wood_collect_map.PNG"));

    public String getImgPath() {
        return imgPath;
    }

    public RssType getRssType() {
        return rssType;
    }

    public String getOnMapIconPath() {
        return onMapIconPath;
    }

    public String getOnMapCollectButtonPath() {
        return onMapCollectButtonPath;
    }

    public static SearchViewButtons getEnumFromRssType(RssType rssType) {
        switch (rssType) {
            case STONE:
                return SearchViewButtons.STONE_ICON;
            case LEAD:
                return SearchViewButtons.LEAD_ICON;
            case FOOD:
                return SearchViewButtons.FOOD_ICON;
            case IRON:
                return SearchViewButtons.IRON_ICON;
            case WOOD:
            default:
                return SearchViewButtons.WOOD_ICON;
        }
    }

    private String imgPath;
    private RssType rssType;
    private String onMapIconPath;
    private String onMapCollectButtonPath;

    SearchViewButtons(String imgPath, Optional<RssType> rssType, Optional<String> onMapIconPath, Optional<String> onMapCollectButtonPath) {
        this.imgPath = imgPath;
        if (rssType.isPresent())
            this.rssType = rssType.get();
        if (onMapIconPath.isPresent())
            this.onMapIconPath = onMapIconPath.get();
        if (onMapCollectButtonPath.isPresent())
            this.onMapCollectButtonPath = onMapCollectButtonPath.get();
    }
}
