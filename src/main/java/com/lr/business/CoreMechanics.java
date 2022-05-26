package com.lr.business;


import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static com.lr.utils.ScreenUtils.takeScreenCapture;
import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
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

    @Autowired
    public CoreMechanics(Robot robot) {
        this.robot = robot;
    }


    public void findAndFarm(int rssLevel, RssType rssType, WinUtils.WindowInfo windowInfo, boolean hasEncampment) throws InterruptedException, AWTException, IOException, URISyntaxException {

        //gotoMainMap with keystroke

        moveAndClick(mainMapButtonsCoordsMap.get(windowInfo).get(MainMapButtons.SEARCH));

        String searchViewPath = takeScreenCapture(windowInfo);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {

            //Extra step to pick first loction
            if(hasEncampment){
                Double[] fortressSelectorCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_RADIO.getImgPath(), searchScreen, windowInfo);
                moveAndClick(fortressSelectorCoords);
            }


            Double[] rssExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo);

            moveAndClick(rssExpander);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] rssTypeChoice = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getImgPath(), searchScreen, windowInfo);
            log.info("Coords for rss expander for rss {} found at: {}" ,rssType, rssTypeChoice);
            moveAndClick(rssTypeChoice);

            Double[] searchOnMapCoords = findCoordsOnScreen(SearchViewButtons.SEARCH_MAP.getImgPath(), searchScreen, windowInfo);
            moveAndClick(searchOnMapCoords);

            String searchResultsPath = takeScreenCapture(windowInfo);
            Mat searchResultsScreen = Imgcodecs.imread(searchResultsPath, CONVERT_IMG_FLAG);

            Double[] goCoords = findCoordsOnScreen(SearchViewButtons.GO_RSS.getImgPath(), searchResultsScreen, windowInfo);
            moveAndClick(goCoords);

            // Now on map


            String mapPath = takeScreenCapture(windowInfo);
            Mat mapScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] rssSource = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getOnMapIconPath(), mapScreen, windowInfo);
            moveAndClick(rssSource);

            mapPath = takeScreenCapture(windowInfo);
            mapScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] rssCollectSource = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getOnMapCollectButtonPath(), mapScreen, windowInfo);
            moveAndClick(rssCollectSource);


            // Now on army selector view
            //FIXME cover the extra screen in case of encampement built

            String armySelectionPath = takeScreenCapture(windowInfo);
            Mat armySelectionScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] armyPresetCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_ICON.getImgPath(), armySelectionScreen, windowInfo);
            moveAndClick(armyPresetCoords);

            String armyPresetsPath = takeScreenCapture(windowInfo);
            Mat armyPresetsScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);
            Double[] armyPresetGatheringCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_RADIO.getImgPath(), armyPresetsScreen, windowInfo);
            moveAndClick(armyPresetGatheringCoords);

            armySelectionPath = takeScreenCapture(windowInfo);
            armySelectionScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] launchCoords = findCoordsOnScreen(ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON.getImgPath(), armySelectionScreen, windowInfo);
            moveAndClick(launchCoords);
        }
        catch(ImageNotMatchedException e){
            log.error(e.getMessage());

        }
        //Back to main screen
        Thread.sleep(2000);

        log.info("Done with findAndFarm");

    }

    private void moveAndClick(Double[] coords) throws InterruptedException {
        robot.mouseMove(coords[0].intValue(), coords[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);
        Thread.sleep(5000);

    }


}
