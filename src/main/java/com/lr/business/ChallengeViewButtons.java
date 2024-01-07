package com.lr.business;

public enum ChallengeViewButtons {

    PAST_CHALLENGE_TAB_FR("past_challenges_fr.PNG"),
    PAST_CHALLENGE_CONTRIBS_BTTN_FR("challenge_contribs_bttn_fr.PNG"),
    PAST_CHALLENGE_HORDE_BANNER_FR("challenge_horde_banner_fr.PNG"),
    PAST_CHALLENGE_LEGION_BANNER_FR("challenge_legion_banner_fr.PNG"),
    PAST_CHALLENGE_ALLIANCE_BANNER_FR("challenge_alliance_banner_fr.PNG"),
    PAST_CHALLENGE_INFERNAL_BANNER_FR("challenge_infernal_banner_fr.PNG");


    private final String imgPath;

    public String getImgPath() {
        return imgPath;
    }

    ChallengeViewButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
