package com.lr.business;


import com.lr.utils.WinUtils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static com.lr.utils.ScreenUtils.takeScreenCapture;
import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGR;

@Component
public class CoreMechanics {


    private Robot robot;

    public void setMainMapButtonsCoordsMap(Map<MainMapButtons, Double[]> mainMapButtonsCoordsMap) {
        this.mainMapButtonsCoordsMap = mainMapButtonsCoordsMap;
    }

    private Map<MainMapButtons, Double[]> mainMapButtonsCoordsMap;

    @Autowired
    public CoreMechanics(Robot robot) {
        this.robot = robot;
    }


    public void findAndFarm(int rssLevel, RssType rssType, WinUtils.WindowInfo windowInfo) throws InterruptedException, AWTException, IOException, URISyntaxException {

        //gotoMainMap with keystroke

        moveAndClick(mainMapButtonsCoordsMap.get(MainMapButtons.SEARCH));

        String searchViewPath = takeScreenCapture(windowInfo);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, COLOR_RGB2BGR);

        Double[] rssExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo);

        moveAndClick(rssExpander);

        searchViewPath = takeScreenCapture(windowInfo);
        searchScreen = Imgcodecs.imread(searchViewPath, COLOR_RGB2BGR);

        Double[] rssTypeChoice = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getImgPath(), searchScreen, windowInfo);
        System.out.println("Coords for rss expander for rss " + rssType + " found at:" + rssTypeChoice);
        moveAndClick(rssTypeChoice);

        Double[] searchOnMapCoords = findCoordsOnScreen(SearchViewButtons.SEARCH_MAP.getImgPath(), searchScreen, windowInfo);
        moveAndClick(searchOnMapCoords);

        String searchResultsPath = takeScreenCapture(windowInfo);
        Mat searchResultsScreen = Imgcodecs.imread(searchResultsPath, COLOR_RGB2BGR);

        Double[] goCoords = findCoordsOnScreen(SearchViewButtons.GO_RSS.getImgPath(), searchResultsScreen, windowInfo);
        moveAndClick(goCoords);

        // Now on map

        String mapPath = takeScreenCapture(windowInfo);
        Mat mapScreen = Imgcodecs.imread(mapPath, COLOR_RGB2BGR);

        Double[] rssSource = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getOnMapIconPath(), mapScreen, windowInfo);
        moveAndClick(rssSource);

        mapPath = takeScreenCapture(windowInfo);
        mapScreen = Imgcodecs.imread(mapPath, COLOR_RGB2BGR);

        Double[] rssCollectSource = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getOnMapCollectButtonPath(), mapScreen, windowInfo);
        moveAndClick(rssCollectSource);


        // Now on army selector view

        String armySelectionPath = takeScreenCapture(windowInfo);
        Mat armySelectionScreen = Imgcodecs.imread(mapPath, COLOR_RGB2BGR);

        Double[] armyPresetCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_ICON.getImgPath(), armySelectionScreen, windowInfo);
        moveAndClick(armyPresetCoords);

        String armyPresetsPath = takeScreenCapture(windowInfo);
        Mat armyPresetsScreen = Imgcodecs.imread(mapPath, COLOR_RGB2BGR);
        Double[] armyPresetGatheringCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_RADIO.getImgPath(), armyPresetsScreen, windowInfo);
        moveAndClick(armyPresetGatheringCoords);

        armySelectionPath = takeScreenCapture(windowInfo);
        armySelectionScreen = Imgcodecs.imread(mapPath, COLOR_RGB2BGR);

        Double[] launchCoords = findCoordsOnScreen(ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON.getImgPath(), armySelectionScreen, windowInfo);
        moveAndClick(launchCoords);

        //Back to main screen
        Thread.sleep(2000);

        System.out.println("Done with findAndFarm");

    }

    private void moveAndClick(Double[] coords) throws InterruptedException {
        robot.mouseMove(coords[0].intValue(), coords[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);
        Thread.sleep(2000);

    }


}
