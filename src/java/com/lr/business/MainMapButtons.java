package com.lr.business;

public enum MainMapButtons {
    SEARCH("search.png"),
//    QUESTS("challenges.png"),
    TRAININGS("trainings.PNG"),
    ALLIANCE ("alliance.PNG")
    ;

    public String getImgPath() {
        return imgPath;
    }

    private String imgPath;

    MainMapButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
