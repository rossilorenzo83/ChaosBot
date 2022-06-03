package com.lr.business;

public enum MainMapButtons {
    SEARCH("search.PNG"),
//    QUESTS("challenges.png"),
    TRAININGS("trainings.PNG"),
    ALLIANCE ("alliance.PNG"),
    ENCAMPMENTS("encampments_map_icon.PNG")
    ;

    public String getImgPath() {
        return imgPath;
    }

    private String imgPath;

    MainMapButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
