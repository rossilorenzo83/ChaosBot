package com.lr.business;


import com.lr.config.GeneralConfig;
import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static com.lr.utils.ScreenUtils.takeScreenCapture;
import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR;

@Component
@Slf4j
public class CoreMechanics {


    private Robot robot;
    public static int CONVERT_IMG_FLAG = IMREAD_COLOR;

    public void setMainMapButtonsCoordsMap(ConcurrentMap<String, Map<MainMapButtons, Double[]>> mainMapButtonsCoordsMap) {
        this.mainMapButtonsCoordsMap = mainMapButtonsCoordsMap;
    }

    public ConcurrentMap<String, Map<MainMapButtons, Double[]>> getMainMapButtonsCoordsMap() {
        return this.mainMapButtonsCoordsMap;
    }

    private ConcurrentMap<String, Map<MainMapButtons, Double[]>> mainMapButtonsCoordsMap;


    private GeneralConfig generalConfig;

    @Autowired
    public CoreMechanics(Robot robot, GeneralConfig generalConfig) {
        this.robot = robot;
        this.generalConfig = generalConfig;
    }


    public void findAndFarm(int rssLevel, RssType rssType, WinUtils.WindowInfo windowInfo, boolean hasEncampment) throws InterruptedException, AWTException, IOException, URISyntaxException {

        //gotoMainMap with keystroke

        moveAndClick(mainMapButtonsCoordsMap.get(windowInfo.getTitle()).get(MainMapButtons.SEARCH));

        String searchViewPath = takeScreenCapture(windowInfo);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {


            Double[] rssExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false);

            moveAndClick(rssExpander);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] rssTypeChoice = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getImgPath(), searchScreen, windowInfo, false);
            log.info("Coords for rss expander for rss {} found at: {}", rssType, rssTypeChoice);
            moveAndClick(rssTypeChoice);

            Double[] searchOnMapCoords = findCoordsOnScreen(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_FR.getImgPath() : SearchViewButtons.SEARCH_MAP_EN.getImgPath(), searchScreen, windowInfo, false);
            moveAndClick(searchOnMapCoords);

            String searchResultsPath = takeScreenCapture(windowInfo);
            Mat searchResultsScreen = Imgcodecs.imread(searchResultsPath, CONVERT_IMG_FLAG);

            Double[] goCoords = findCoordsOnScreen(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_FR.getImgPath() : SearchViewButtons.GO_RSS_EN.getImgPath(), searchResultsScreen, windowInfo, false);
            moveAndClick(goCoords);

            // Now on map
            Double[] rssSource = findWindowCenterCoords(windowInfo);
            moveAndClick(rssSource);

            String mapPath = takeScreenCapture(windowInfo);
            Mat mapScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] rssCollectSource = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getOnMapCollectButtonPath(), mapScreen, windowInfo, true);
            moveAndClick(rssCollectSource);


            // Now on army selector view
            if (hasEncampment) {
                handleStartLocationScreen(windowInfo);
            }

            String armySelectionPath = takeScreenCapture(windowInfo);
            Mat armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] armyPresetCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_ICON.getImgPath(), armySelectionScreen, windowInfo, false);
            moveAndClick(armyPresetCoords);

            String armyPresetsPath = takeScreenCapture(windowInfo);
            Mat armyPresetsScreen = Imgcodecs.imread(armyPresetsPath, CONVERT_IMG_FLAG);
            Double[] armyPresetGatheringCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_RADIO.getImgPath(), armyPresetsScreen, windowInfo, false);
            moveAndClick(armyPresetGatheringCoords);

            armySelectionPath = takeScreenCapture(windowInfo);
            armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] launchCoords = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), armySelectionScreen, windowInfo, false);
            moveAndClick(launchCoords);
        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());
            //Go back to main screen
            if (!e.getInMainMap()) {
                goBackToMainMap();
            }


        }
        //Back to main screen
        Thread.sleep(generalConfig.getActionIntervalMs());

        log.info("Done with findAndFarm");

    }

    public void armyFarming(int armyLvl, int armyPreset, WinUtils.WindowInfo windowInfo, boolean hasEncampment) throws IOException, AWTException, InterruptedException, URISyntaxException {

        moveAndClick(mainMapButtonsCoordsMap.get(windowInfo.getTitle()).get(MainMapButtons.SEARCH));

        String searchViewPath = takeScreenCapture(windowInfo);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {

            Double[] rssExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false);
            Double[] mapSearchButton = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_EN.getImgPath() : SearchViewButtons.SEARCH_MAP_FR.getImgPath(), searchScreen, windowInfo, false);

            moveAndClick(rssExpander);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] armyChoice = findCoordsOnScreen(SearchViewButtons.ARMY_ICON.getImgPath(), searchScreen, windowInfo, false);
            moveAndClick(armyChoice);

            moveAndClick(mapSearchButton);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] goToArmy = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_EN.getImgPath() : SearchViewButtons.GO_RSS_FR.getImgPath(), searchScreen, windowInfo, false);
            moveAndClick(goToArmy);

            Double[] armyOnMap = findWindowCenterCoords(windowInfo);
            moveAndClick(armyOnMap);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] attackBtn = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, true);
            moveAndClick(attackBtn);

            if (hasEncampment) {
                handleStartLocationScreen(windowInfo);
            }

            String armySelectionViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);
            log.info("Clicking army preset #{}", armyPreset);
            Double[] armyPresetBtn = findCoordsOnScreen(ExpeditionViewButtons.getPresetById(armyPreset).getImgPath(), searchScreen, windowInfo, false);
            moveAndClick(armyPresetBtn);

            armySelectionViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);
            Double[] launchPartyButton = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false);

            moveAndClick(launchPartyButton);


        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());

        }
        //Back to main screen
        Thread.sleep(generalConfig.getActionIntervalMs());

        log.info("Done with farmArmies");

    }

    private void handleStartLocationScreen(WinUtils.WindowInfo windowInfo) throws AWTException, IOException, URISyntaxException, ImageNotMatchedException, InterruptedException {
        String locationSelectionPath = takeScreenCapture(windowInfo);
        Mat locationSelectionScreen = Imgcodecs.imread(locationSelectionPath, CONVERT_IMG_FLAG);
        Double[] fortressIcon = findCoordsOnScreen(ExpeditionViewButtons.FORTRESS_SELECTION_ICON.getImgPath(), locationSelectionScreen, windowInfo, false);
        moveAndClick(fortressIcon);
        Double[] nextBtnCoords = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.NEXT_BUTTON_EN.getImgPath() : ExpeditionViewButtons.NEXT_BUTTON_FR.getImgPath(), locationSelectionScreen, windowInfo, false);
        moveAndClick(nextBtnCoords);
    }

    private Double[] findWindowCenterCoords(WinUtils.WindowInfo windowInfo) {
        Rectangle screenRect = new Rectangle(windowInfo.getRect().left, windowInfo.getRect().top, Math.abs(windowInfo.getRect().right
                - windowInfo.getRect().left), Math.abs(windowInfo.getRect().bottom - windowInfo.getRect().top));
        return new Double[]{screenRect.getX() + screenRect.getWidth() / 2, screenRect.getY() + screenRect.getHeight() / 2};
    }

    private void moveAndClick(Double[] coords) throws InterruptedException {
        robot.mouseMove(coords[0].intValue(), coords[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);
        Thread.sleep(generalConfig.getActionIntervalMs());

    }

    private void goBackToMainMap() throws InterruptedException {
        robot.keyPress(VK_ESCAPE);
        robot.keyRelease(VK_ESCAPE);
        Thread.sleep(generalConfig.getActionIntervalMs());

        robot.keyPress(VK_ESCAPE);
        robot.keyRelease(VK_ESCAPE);
        Thread.sleep(generalConfig.getActionIntervalMs());
    }


}
