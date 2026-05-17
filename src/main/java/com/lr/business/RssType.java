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

    private static final RssType[] ALL_EXCEPT_RELIC = Arrays.stream(values())
            .filter(r -> r != RELIC)
            .toArray(RssType[]::new);

    public static RssType[] standardTypes() {
        return STANDARD_TYPES;
    }

    public static RssType[] allExceptRelic() {
        return ALL_EXCEPT_RELIC;
    }
}
