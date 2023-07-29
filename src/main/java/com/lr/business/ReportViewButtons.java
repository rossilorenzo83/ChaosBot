package com.lr.business;

public enum ReportViewButtons {

    MARCH_REPORTS_TAB_FR("march_reps_tab_fr.PNG"),
    MARCH_REPORTS_TAB_EN("march_reps_tab_fr.PNG"),
    RSS_RECEIVED_FR ("rss_received_fr.PNG"),
    RSS_RECEIVED_EN ("rss_received_fr.PNG");

    private final String imgPath;

    public String getImgPath() {
        return imgPath;
    }

    ReportViewButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
