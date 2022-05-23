package com.lr.business;

public enum ExpeditionViewButtons {

    PRESET_ICON("wb_presets.PNG"),
    PRESET_RADIO("preset_radio.PNG"),
    LAUNCH_EXPEDITION_BUTTON("launch_wb_fr.PNG");

    public String getImgPath() {
        return imgPath;
    }

    private String imgPath;

    ExpeditionViewButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
