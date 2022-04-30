package com.lr.business;

public enum SearchViewButtons {

    SEARCH_EXPANDER("search_type_expander.PNG"),
    STONE_ICON("stone_icon.PNG");

    public String getImgPath() {
        return imgPath;
    }

    private String imgPath;

    SearchViewButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
