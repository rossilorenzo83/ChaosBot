package com.lr.business;

import java.util.Arrays;

public enum RssType {
    IRON(false),
    STONE(false),
    FOOD(false),
    LEAD(false),
    WOOD(false),
    WARPSTONE(true),
    RELIC(true);

    private final boolean event;

    RssType(boolean event) {
        this.event = event;
    }

    public boolean isEvent() {
        return event;
    }

    private static final RssType[] STANDARD_TYPES = Arrays.stream(values())
            .filter(r -> !r.event)
            .toArray(RssType[]::new);

    public static RssType[] standardTypes() {
        return STANDARD_TYPES;
    }
}
