package com.lr.business;

public enum MainMapButtons {
    SEARCH("search.png"),
    QUESTS(""),
    CHALLENGES("icon_challenges"),
    ALLIANCE ("alliance.PNG"),
    SEARCH_EXPANDER("search_type_expander.PNG"),
    STONE_ICON("stone_icon.PNG");

    public String getImgPath() {
        return imgPath;
    }

    private String imgPath;

    MainMapButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
