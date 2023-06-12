package com.lr.business;

public enum ExpeditionViewButtons {

    PRESET_ICON("wb_presets.PNG"),
    NEXT_BUTTON_EN("next_button_en.PNG"),
    NEXT_BUTTON_FR("next_button_fr.PNG"),
    PRESET_RADIO("preset_radio.PNG"),
    PRESET_RADIO_1("army_preset_1.PNG"),
    PRESET_RADIO_2("army_preset_2.PNG"),
    PRESET_RADIO_3("army_preset_3.PNG"),
    PRESET_RADIO_4("army_preset_4.PNG"),

    HERO_SLIDER("hero_select_slider.PNG"),
    FORTRESS_SELECTION_ICON("fortress_selection_icon.PNG"),
    LAUNCH_EXPEDITION_BUTTON_EN("launch_march_en.PNG"),
    LAUNCH_EXPEDITION_BUTTON_FR("launch_march_fr.PNG"),
    LAUNCH_ATTACK_BUTTON_EN("launch_attack_en.PNG"),
    LAUNCH_ATTACK_BUTTON_FR("launch_attack_fr.PNG"),
    CONFIRM_ATTACK_BUTTON_EN("confirm_army_too_powerful_fr.PNG"),
    CONFIRM_ATTACK_BUTTON_FR("confirm_army_too_powerful_fr.PNG");

    public String getImgPath() {
        return imgPath;
    }

    public static ExpeditionViewButtons getPresetById(int presetId) {
        switch (presetId) {
            case 1:
                return PRESET_RADIO_1;
            case 2:
                return PRESET_RADIO_2;
            case 3:
                return PRESET_RADIO_3;
            case 4:
            default:
                return PRESET_RADIO_4;

        }
    }

    private String imgPath;

    ExpeditionViewButtons(String imgPath) {
        this.imgPath = imgPath;
    }
}
