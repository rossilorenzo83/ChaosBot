package com.lr.business;


import com.lr.config.GeneralConfig;
import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.awt.*;
import java.awt.event.KeyEvent;
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


    public static final int FAT_ARMY_THRESHOLD = 15;
    private final Robot robot;

    private final Tesseract ocrEngine;
    public static int CONVERT_IMG_FLAG = IMREAD_COLOR;

    public void setMainMapButtonsCoordsMap(ConcurrentMap<String, Map<MainMapButtons, Double[]>> mainMapButtonsCoordsMap) {
        this.mainMapButtonsCoordsMap = mainMapButtonsCoordsMap;
    }

    public ConcurrentMap<String, Map<MainMapButtons, Double[]>> getMainMapButtonsCoordsMap() {
        return this.mainMapButtonsCoordsMap;
    }

    private ConcurrentMap<String, Map<MainMapButtons, Double[]>> mainMapButtonsCoordsMap;


    private final GeneralConfig generalConfig;

    private final ResourceLoader resourceLoader;

    @Autowired
    public CoreMechanics(Robot robot, Tesseract ocrEngine, GeneralConfig generalConfig, ResourceLoader resourceLoader) {
        this.robot = robot;
        this.ocrEngine = ocrEngine;
        this.generalConfig = generalConfig;
        this.resourceLoader = resourceLoader;
    }


    public void findAndFarm(String rssLevel, RssType rssType, WinUtils.WindowInfo windowInfo, boolean hasEncampment) throws InterruptedException, AWTException, IOException, URISyntaxException {

        //gotoMainMap with keystroke

        moveAndClick(mainMapButtonsCoordsMap.get(windowInfo.getTitle()).get(MainMapButtons.SEARCH));

        String searchViewPath = takeScreenCapture(windowInfo);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {


            Double[] rssExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            Double[] lvlChoiceExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());


            moveAndClick(rssExpander);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] rssTypeChoice = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            log.info("Coords for rss expander for rss {} found at: {}", rssType, rssTypeChoice);
            moveAndClick(rssTypeChoice);

            moveAndClick(lvlChoiceExpander);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] lvlChoice = findCoordsOnScreen(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getLevelIconImgPath(rssLevel, generalConfig.getGameLanguage()), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(lvlChoice);


            Double[] searchOnMapCoords = findCoordsOnScreen(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_FR.getImgPath() : SearchViewButtons.SEARCH_MAP_EN.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(searchOnMapCoords);

            String searchResultsPath = takeScreenCapture(windowInfo);
            Mat searchResultsScreen = Imgcodecs.imread(searchResultsPath, CONVERT_IMG_FLAG);

            Double[] goCoords = findCoordsOnScreen(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_FR.getImgPath() : SearchViewButtons.GO_RSS_EN.getImgPath(), searchResultsScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(goCoords);

            // Now on map
            Double[] rssSource = findWindowCenterCoords(windowInfo);
            moveAndClick(rssSource);

            String mapPath = takeScreenCapture(windowInfo);
            Mat mapScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] rssCollectSource = findCoordsOnScreen(SearchViewButtons.getEnumFromRssType(rssType).getOnMapCollectButtonPath(), mapScreen, windowInfo, true, generalConfig.getImageQualityLowerBound());
            moveAndClick(rssCollectSource);


            // Now on army selector view
            if (hasEncampment) {
                handleStartLocationScreen(windowInfo);
            }

            String armySelectionPath = takeScreenCapture(windowInfo);
            Mat armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] armyPresetCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_ICON.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            try {
                Double[] heroSliderCoords = findCoordsOnScreen(ExpeditionViewButtons.HERO_SLIDER.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                moveAndClick(heroSliderCoords);
            } catch (ImageNotMatchedException e) {
                log.info("Hero not avail");
            }

            moveAndClick(armyPresetCoords);

            String armyPresetsPath = takeScreenCapture(windowInfo);
            Mat armyPresetsScreen = Imgcodecs.imread(armyPresetsPath, CONVERT_IMG_FLAG);
            Double[] armyPresetGatheringCoords = findCoordsOnScreen(ExpeditionViewButtons.PRESET_RADIO.getImgPath(), armyPresetsScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(armyPresetGatheringCoords);

            armySelectionPath = takeScreenCapture(windowInfo);
            armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] launchCoords = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
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

    public void armyFarming(String armyLvl, int armyPreset, WinUtils.WindowInfo windowInfo, boolean hasEncampment) throws IOException, AWTException, InterruptedException, URISyntaxException {

        moveAndClick(mainMapButtonsCoordsMap.get(windowInfo.getTitle()).get(MainMapButtons.SEARCH));

        String searchViewPath = takeScreenCapture(windowInfo);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {

            Double[] rssExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            Double[] mapSearchButton = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_EN.getImgPath() : SearchViewButtons.SEARCH_MAP_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            moveAndClick(rssExpander);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] lvlChoiceExpander = findCoordsOnScreen(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());


            Double[] armyChoice = findCoordsOnScreen(SearchViewButtons.ARMY_ICON.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(armyChoice);

            moveAndClick(lvlChoiceExpander);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] lvlChoice = findCoordsOnScreen(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getLevelIconImgPath(armyLvl, generalConfig.getGameLanguage()), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(lvlChoice);


            moveAndClick(mapSearchButton);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] goToArmy = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_EN.getImgPath() : SearchViewButtons.GO_RSS_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(goToArmy);

            Double[] armyOnMap = findWindowCenterCoords(windowInfo);
            moveAndClick(armyOnMap);

            searchViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] attackBtn = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, true, generalConfig.getImageQualityLowerBound());
            moveAndClick(attackBtn);

            if (hasEncampment) {
                handleStartLocationScreen(windowInfo);
            }

            String armySelectionViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);
            log.info("Clicking army preset #{}", armyPreset);
            Double[] armyPresetBtn = findCoordsOnScreen(ExpeditionViewButtons.getPresetById(armyPreset).getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(armyPresetBtn);

            armySelectionViewPath = takeScreenCapture(windowInfo);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);

            Double[] launchPartyButton = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            moveAndClick(launchPartyButton);

            if(Integer.parseInt(armyLvl) >= FAT_ARMY_THRESHOLD) {
                armySelectionViewPath = takeScreenCapture(windowInfo);
                searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);

                Double[] launchPartyConfirmationButton = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                moveAndClick(launchPartyConfirmationButton);
            }

        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());
            //Go back to main screen
            if (!e.getInMainMap()) {
                goBackToMainMap();
            }

        }
        //Back to main screen
        Thread.sleep(generalConfig.getActionIntervalMs());

        log.info("Done with farmArmies");

    }

    public void challengeStats(WinUtils.WindowInfo windowInfo, WebClient discordWebClient, ChallengeViewButtons challengeViewButtons) throws IOException, AWTException, URISyntaxException, InterruptedException, TesseractException {

        //Get focus
        moveAndClick(findWindowCenterCoords(windowInfo));


        try {
            robot.keyPress(KeyEvent.VK_D);
            robot.keyRelease(KeyEvent.VK_D);
            Thread.sleep(generalConfig.getActionIntervalMs());

            String challengePage = takeScreenCapture(windowInfo);
            Mat locationSelectionScreen = Imgcodecs.imread(challengePage, CONVERT_IMG_FLAG);
            Double[] pastChallengeCoords = findCoordsOnScreen(ChallengeViewButtons.PAST_CHALLENGE_TAB_FR.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            moveAndClick(pastChallengeCoords);

            String pastChallengePage = takeScreenCapture(windowInfo);
            Mat pastChallengePageMat = Imgcodecs.imread(pastChallengePage, CONVERT_IMG_FLAG);
            Mat pastChallengeCurrentPageMat = pastChallengePageMat;

            Double[] bottomCoords = findWindowBottomCoords(windowInfo);
            robot.mouseMove(bottomCoords[0].intValue(), bottomCoords[1].intValue());
            Thread.sleep(generalConfig.getActionIntervalMs());

            int mainScrollCounter = 0;
            boolean prevNotFound = false;
            do {


                pastChallengePageMat = pastChallengeCurrentPageMat;


                    try {
                        Double[] coords = findCoordsOnScreen(challengeViewButtons.getImgPath(), pastChallengePageMat, windowInfo, false, generalConfig.getImageQualityLowerBound());
                        moveAndClick(coords);

                        MultipartBodyBuilder discordRestbuilder = new MultipartBodyBuilder();


                        String challengeDetailsScreenCapturePath = takeScreenCapture(windowInfo);

                        int scrollCounter = 0;
                        discordRestbuilder.part("files[" + scrollCounter + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + ".jpg"));

//        StringBuffer contextText = new StringBuffer("Stats for challenge:\n");

                        Mat challengeDetailsScreenCapture = Imgcodecs.imread(challengeDetailsScreenCapturePath, CONVERT_IMG_FLAG);
                        coords = findCoordsOnScreen(ChallengeViewButtons.PAST_CHALLENGE_CONTRIBS_BTTN_FR.getImgPath(), challengeDetailsScreenCapture, windowInfo, false, generalConfig.getImageQualityLowerBound());
                        moveAndClick(coords);
                        String challengeScorersScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter);
                        discordRestbuilder.part("files[" + scrollCounter+1 + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + "scores" + scrollCounter + ".jpg"));
                        Mat challengeScorersScreenCapture = Imgcodecs.imread(challengeDetailsScreenCapturePath, CONVERT_IMG_FLAG);
                        Mat challengeScorersCurrentScreenCapture = challengeScorersScreenCapture;

                        do {

                            bottomCoords = findWindowBottomCoords(windowInfo);
                            robot.mouseMove(bottomCoords[0].intValue(), bottomCoords[1].intValue());
                            Thread.sleep(generalConfig.getActionIntervalMs());


                            challengeScorersScreenCapture = challengeScorersCurrentScreenCapture;

//            contextText.append(ScreenUtils.extractTextFromImage(challengeScorersScreenCapturePath, ocrEngine, resourceLoader));


                            robot.mouseWheel(3);
                            Thread.sleep(generalConfig.getActionIntervalMs());

                            scrollCounter++;
                            String challengeScorersCurrentScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter);
                            discordRestbuilder.part("files[" + scrollCounter+1 + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + "scores" + scrollCounter + ".jpg"));
                            challengeScorersCurrentScreenCapture = Imgcodecs.imread(challengeScorersCurrentScreenCapturePath, CONVERT_IMG_FLAG);
                            challengeScorersScreenCapturePath = challengeScorersCurrentScreenCapturePath;
                        }
                        while (scrollCounter < 3);

//        contextText.append(ScreenUtils.extractTextFromImage(challengeScorersScreenCapturePath, ocrEngine, resourceLoader));
//        discordRestbuilder.part("content", contextText.toString());

                        if(!generalConfig.isPostDryRun()) {
                            log.info("Publish on discord");
                            try {
                                discordWebClient.post().uri("/messages").body(BodyInserters.fromMultipartData(discordRestbuilder.build())).retrieve().bodyToMono(String.class).block();

                            } catch (WebClientException e) {
                                log.error("Error calling discord api: {}", e.getMessage());
                            }
                        }


                        robot.keyPress(VK_ESCAPE);
                        robot.keyRelease(VK_ESCAPE);
                        Thread.sleep(generalConfig.getActionIntervalMs());

                        robot.keyPress(VK_ESCAPE);
                        robot.keyRelease(VK_ESCAPE);
                        Thread.sleep(generalConfig.getActionIntervalMs());
                        prevNotFound = false;


                    } catch (ImageNotMatchedException e) {
                        log.info("Challenge not found move fwd");
                        prevNotFound = true;

                    }

                mainScrollCounter++;
                log.info("Arrived at {} scrolls", mainScrollCounter);
                //Scroll
                bottomCoords = findWindowBottomCoords(windowInfo);
                robot.mouseMove(bottomCoords[0].intValue(), bottomCoords[1].intValue());
                Thread.sleep(generalConfig.getActionIntervalMs());

                if (prevNotFound) {
                    robot.mouseWheel(1);
                    Thread.sleep(generalConfig.getActionIntervalMs());
                } else {
                    log.info("Scrolling {} times", mainScrollCounter);
                    for(int i=0; i<mainScrollCounter ;i++){
                        robot.mouseWheel(1);
                        Thread.sleep(generalConfig.getActionIntervalMs());
                    }
                }

                String pastChallengeCurrentPage = takeScreenCapture(windowInfo);
                pastChallengeCurrentPageMat = Imgcodecs.imread(pastChallengeCurrentPage, CONVERT_IMG_FLAG);
            }
            while (mainScrollCounter < 20);

            //Get back to map screen
            robot.keyPress(VK_ESCAPE);
            robot.keyRelease(VK_ESCAPE);
            Thread.sleep(generalConfig.getActionIntervalMs());

        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());
        }
    }

    private void handleStartLocationScreen(WinUtils.WindowInfo windowInfo) throws AWTException, IOException, URISyntaxException, ImageNotMatchedException, InterruptedException {
        String locationSelectionPath = takeScreenCapture(windowInfo);
        Mat locationSelectionScreen = Imgcodecs.imread(locationSelectionPath, CONVERT_IMG_FLAG);
        Double[] fortressIcon = findCoordsOnScreen(ExpeditionViewButtons.FORTRESS_SELECTION_ICON.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
        moveAndClick(fortressIcon);
        Double[] nextBtnCoords = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.NEXT_BUTTON_EN.getImgPath() : ExpeditionViewButtons.NEXT_BUTTON_FR.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
        moveAndClick(nextBtnCoords);
    }

    public static Double[] findWindowCenterCoords(WinUtils.WindowInfo windowInfo) {
        Rectangle screenRect = new Rectangle(windowInfo.getRect().left, windowInfo.getRect().top, Math.abs(windowInfo.getRect().right
                - windowInfo.getRect().left), Math.abs(windowInfo.getRect().bottom - windowInfo.getRect().top));
        return new Double[]{screenRect.getX() + screenRect.getWidth() / 2, screenRect.getY() + screenRect.getHeight() / 2};
    }

    public static Double[] findWindowBottomCoords(WinUtils.WindowInfo windowInfo) {
        Rectangle screenRect = new Rectangle(windowInfo.getRect().left, windowInfo.getRect().top, Math.abs(windowInfo.getRect().right
                - windowInfo.getRect().left), Math.abs(windowInfo.getRect().bottom - windowInfo.getRect().top));
        return new Double[]{screenRect.getX() + screenRect.getWidth() / 2, screenRect.getY() + screenRect.getHeight() * 2 / 3};
    }

    private void moveAndClick(Double[] coords) throws InterruptedException {
        robot.mouseMove(coords[0].intValue(), coords[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);
        Thread.sleep(generalConfig.getActionIntervalMs());

    }

    private void scrollAndExtract() throws InterruptedException {
        robot.mouseWheel(1);
        Thread.sleep(generalConfig.getActionIntervalMs());
    }


    private void goBackToMainMap() throws InterruptedException {
        robot.keyPress(VK_ESCAPE);
        robot.keyRelease(VK_ESCAPE);
        Thread.sleep(generalConfig.getActionIntervalMs());
    }


}
