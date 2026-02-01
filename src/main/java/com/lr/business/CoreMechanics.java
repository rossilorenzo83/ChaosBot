package com.lr.business;


import com.lr.config.GeneralConfig;
import com.lr.utils.ScreenUtils;
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

import static com.lr.utils.ScreenUtils.*;
import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR;

@Component
@Slf4j
public class CoreMechanics {

    @Autowired
    Random random;

    public static final int FAT_ARMY_THRESHOLD = 15;
    public static final int SCROLL_AMOUNT = 20;

    /**
     * Global lock for ALL automation operations.
     *
     * CRITICAL: Even with separate Robot instances per thread, there is only ONE physical
     * mouse cursor on the system. All Robot.mouseMove() calls control the same cursor.
     *
     * This lock ensures that entire automation sequences (find -> click -> wait -> verify)
     * are executed atomically without interference from other threads.
     *
     * Without this, Thread A might:
     *   1. Move mouse to Window A button
     *   2. [Thread B moves mouse to Window B]  <-- RACE CONDITION
     *   3. Click (now clicking wrong location!)
     */
    private final Object globalAutomationLock = new Object();

    /**
     * Gets the global automation lock for serializing multi-window operations.
     * Workers must acquire this lock before performing any automation sequence.
     */
    public Object getGlobalAutomationLock() {
        return globalAutomationLock;
    }

    /**
     * Lock for thread-safe initialization of mainMapButtonsCoordsMap.
     * Used by WindowAutomationWorker during window registration.
     */
    private final Object coordsMapInitLock = new Object();

    /**
     * Getter for coordsMapInitLock to allow WindowAutomationWorker to synchronize on it.
     */
    public Object getCoordsMapInitLock() {
        return coordsMapInitLock;
    }

    private final Tesseract ocrEngine;
    public static final int CONVERT_IMG_FLAG = IMREAD_COLOR;

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
    public CoreMechanics(Tesseract ocrEngine, GeneralConfig generalConfig, ResourceLoader resourceLoader) {
        this.ocrEngine = ocrEngine;
        this.generalConfig = generalConfig;
        this.resourceLoader = resourceLoader;
    }


    public void findAndFarm(String rssLevel, RssType rssType, WinUtils.WindowInfo windowInfo, boolean hasEncampment, Robot robot) throws InterruptedException, AWTException, IOException, URISyntaxException {
        // Acquire global lock - only one window can control mouse at a time
        synchronized (globalAutomationLock) {
            findAndFarmInternal(rssLevel, rssType, windowInfo, hasEncampment, robot);
        }
    }

    private void findAndFarmInternal(String rssLevel, RssType rssType, WinUtils.WindowInfo windowInfo, boolean hasEncampment, Robot robot) throws InterruptedException, AWTException, IOException, URISyntaxException {
        WinUtils.focusWindow(windowInfo.getTitle());
        Thread.sleep(200);

        Map<MainMapButtons, Double[]> windowCoords = mainMapButtonsCoordsMap.get(windowInfo.getTitle());
        if (windowCoords == null) {
            throw new IllegalStateException("No coordinates found for window: " + windowInfo.getTitle());
        }
        Double[] searchCoords = windowCoords.get(MainMapButtons.SEARCH);
        log.info("Window '{}' SEARCH coords: ({}, {})", windowInfo.getTitle(),
            searchCoords != null ? searchCoords[0] : "NULL",
            searchCoords != null ? searchCoords[1] : "NULL");
        moveAndClick(searchCoords, robot);

        String searchViewPath = takeScreenCapture(windowInfo, robot);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {


            Double[] rssExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            Double[] lvlChoiceExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());


            moveAndClick(rssExpander, robot);

            searchViewPath = takeScreenCapture(windowInfo, robot);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            // Event resources (warpstone, relic) require scrolling to find in resource list
            Double[] rssTypeChoice;
            if (rssType.isEvent()) {
                rssTypeChoice = findScrollableRssIconWithScrolling(rssType, searchScreen, windowInfo, rssExpander, robot);
            } else {
                rssTypeChoice = findCoordsOnScreenFlexible(SearchViewButtons.getEnumFromRssType(rssType).getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            }

            log.info("Coords for rss expander for rss {} found at: {}", rssType, rssTypeChoice);
            moveAndClick(rssTypeChoice, robot);

            moveAndClick(lvlChoiceExpander, robot);

            searchViewPath = takeScreenCapture(windowInfo, robot);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            rssLevel = handleRange(rssLevel);

            Double[] lvlChoice = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getLevelIconImgPath(rssLevel, generalConfig.getGameLanguage()), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(lvlChoice, robot);


            Double[] searchOnMapCoords = findCoordsOnScreenFlexible(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_FR.getImgPath() : SearchViewButtons.SEARCH_MAP_EN.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(searchOnMapCoords, robot);

            String searchResultsPath = takeScreenCapture(windowInfo, robot);
            Mat searchResultsScreen = Imgcodecs.imread(searchResultsPath, CONVERT_IMG_FLAG);

            Double[] goCoords = findCoordsOnScreenFlexible(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_FR.getImgPath() : SearchViewButtons.GO_RSS_EN.getImgPath(), searchResultsScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(goCoords, robot);

            // Now on map
            Double[] rssSource = findWindowCenterCoords(windowInfo);
            moveAndClick(rssSource, robot);

            String mapPath = takeScreenCapture(windowInfo, robot);
            Mat mapScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] rssCollectSource = findCoordsOnScreenFlexible(SearchViewButtons.getEnumFromRssType(rssType).getOnMapCollectButtonPath(), mapScreen, windowInfo, true, generalConfig.getImageQualityLowerBound());
            moveAndClick(rssCollectSource, robot);


            // Now on army selector view
            if (hasEncampment) {
                handleStartLocationScreen(windowInfo, robot);
            }

            String armySelectionPath = takeScreenCapture(windowInfo, robot);
            Mat armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] armyPresetCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.PRESET_ICON.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            Double[] qtyLeftCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.RSS_LEFT.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());


            useHeroIfLowQtyNode(qtyLeftCoords, windowInfo, armySelectionScreen, robot);

            moveAndClick(armyPresetCoords, robot);

            String armyPresetsPath = takeScreenCapture(windowInfo, robot);
            Mat armyPresetsScreen = Imgcodecs.imread(armyPresetsPath, CONVERT_IMG_FLAG);
            Double[] armyPresetGatheringCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.PRESET_RADIO.getImgPath(), armyPresetsScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(armyPresetGatheringCoords, robot);

            armySelectionPath = takeScreenCapture(windowInfo, robot);
            armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] launchCoords = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(launchCoords, robot);
        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());
            //Go back to main screen
            if (!e.getInMainMap()) {
                goBackToMainMap(robot);
            }


        }
        //Back to main screen
        Thread.sleep(generalConfig.getActionIntervalMs());

        log.info("Done with findAndFarm");

    }

    private void useHeroIfLowQtyNode(Double[] coords, WinUtils.WindowInfo windowInfo, Mat armySelectionScreen, Robot robot) throws AWTException, IOException, URISyntaxException, InterruptedException {
        try {
            Rectangle rect = new Rectangle(coords[0].intValue() + 10, coords[1].intValue() - 10, 100, 20);
            String qtyPath = takeScreenCapture(rect, "qtyExtract", windowInfo.getTitle(), robot);
            //Treat input as single line text
            ocrEngine.setPageSegMode(7);
            String extractedText = ScreenUtils.extractTextFromImage(qtyPath, ocrEngine);
            log.info("Extracted Text: {}", extractedText);

            String[] splitText = extractedText.trim().split("/");
            String qtyAvail = splitText.length > 1 ? splitText[1].trim() : "";
            log.info("Extracted residual qty: {}", qtyAvail);

            if (qtyAvail.matches("^[0-9]+$") || (qtyAvail.contains("k") && Double.parseDouble(extractSafelyNumberFromOCRString(qtyAvail).replaceAll(",", ".")) < 30)) {
                Double[] heroSliderCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.HERO_SLIDER.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                moveAndClick(heroSliderCoords, robot);
            }

        } catch (ImageNotMatchedException e) {
            log.info("Hero not avail");
        } catch (TesseractException e) {
            log.info("Could not extract qty left on node {}", e.getMessage());
        }
    }

    private static String extractSafelyNumberFromOCRString(String qtyAvail) throws TesseractException {
        String parsedQty = qtyAvail.split("k")[0];
        if (parsedQty.matches("^[0-9.,]+$"))
            return parsedQty;
        else throw new TesseractException("Parsed qty isn't a number");
    }

    public void armyFarming(String armyLvl, int armyPreset, WinUtils.WindowInfo windowInfo, boolean hasEncampment, Boolean isSkelly, Boolean isFirstRun, Robot robot) throws IOException, AWTException, InterruptedException, URISyntaxException {
        // Acquire global lock - only one window can control mouse at a time
        synchronized (globalAutomationLock) {
            armyFarmingInternal(armyLvl, armyPreset, windowInfo, hasEncampment, isSkelly, isFirstRun, robot);
        }
    }

    private void armyFarmingInternal(String armyLvl, int armyPreset, WinUtils.WindowInfo windowInfo, boolean hasEncampment, Boolean isSkelly, Boolean isFirstRun, Robot robot) throws IOException, AWTException, InterruptedException, URISyntaxException {
        WinUtils.focusWindow(windowInfo.getTitle());
        Thread.sleep(200);

        Map<MainMapButtons, Double[]> windowCoords = mainMapButtonsCoordsMap.get(windowInfo.getTitle());
        if (windowCoords == null) {
            throw new IllegalStateException("No coordinates found for window: " + windowInfo.getTitle());
        }
        Double[] searchCoords = windowCoords.get(MainMapButtons.SEARCH);
        log.info("Window '{}' SEARCH coords (armyFarming): ({}, {})", windowInfo.getTitle(),
            searchCoords != null ? searchCoords[0] : "NULL",
            searchCoords != null ? searchCoords[1] : "NULL");
        moveAndClick(searchCoords, robot);
        String searchViewPath = takeScreenCapture(windowInfo, robot);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {
            Double[] mapSearchButton = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_EN.getImgPath() : SearchViewButtons.SEARCH_MAP_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            if (isFirstRun) {
                Double[] rssExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                moveAndClick(rssExpander, robot);
                searchViewPath = takeScreenCapture(windowInfo, robot);
                searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

                Double[] lvlChoiceExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());


                Double[] armyChoice = findCoordsOnScreenFlexible(SearchViewButtons.ARMY_ICON.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                moveAndClick(armyChoice, robot);

                moveAndClick(lvlChoiceExpander, robot);

                searchViewPath = takeScreenCapture(windowInfo, robot);
                searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);


                armyLvl = handleRange(armyLvl);

                Double[] lvlChoice = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getLevelIconImgPath(armyLvl, generalConfig.getGameLanguage()), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                moveAndClick(lvlChoice, robot);
            }

            moveAndClick(mapSearchButton, robot);

            searchViewPath = takeScreenCapture(windowInfo, robot);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            if (isSkelly != null && isSkelly) {
                //Make sure we take only skelly army
                Double[] skellyCoord = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_ARMY_SKELLY.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                Double[] extrapolateGoIconCoords = computeGoIconForSpecificArmy(skellyCoord, windowInfo, Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_EN.getImgPath() : SearchViewButtons.GO_RSS_FR.getImgPath(), searchScreen);
                log.info("Computed coord for go button on skelly army row: {}x{}", extrapolateGoIconCoords[0], extrapolateGoIconCoords[1]);
                moveAndClick(extrapolateGoIconCoords, robot);
            } else {
                Double[] goToArmy = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_EN.getImgPath() : SearchViewButtons.GO_RSS_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                moveAndClick(goToArmy, robot);

            }

            Double[] armyOnMap = findWindowCenterCoords(windowInfo);
            moveAndClick(armyOnMap, robot);

            searchViewPath = takeScreenCapture(windowInfo, robot);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] attackBtn = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, true, generalConfig.getImageQualityLowerBound());
            moveAndClick(attackBtn, robot);

            if (hasEncampment) {
                handleStartLocationScreen(windowInfo, robot);
            }

            String armySelectionViewPath = takeScreenCapture(windowInfo, robot);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);
            log.info("Clicking army preset #{}", armyPreset);
            Double[] armyPresetBtn = findCoordsOnScreenFlexible(ExpeditionViewButtons.getPresetById(armyPreset).getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            moveAndClick(armyPresetBtn, robot);

            armySelectionViewPath = takeScreenCapture(windowInfo, robot);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);


            try {

                Double[] launchPartyButton = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                moveAndClick(launchPartyButton, robot);
            } catch (ImageNotMatchedException e) {

                //Use case for big army warning
                if (!"ALL".equalsIgnoreCase(armyLvl) && Integer.parseInt(armyLvl) >= FAT_ARMY_THRESHOLD) {
                    armySelectionViewPath = takeScreenCapture(windowInfo, robot);
                    searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);

                    Double[] launchPartyConfirmationButton = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                    moveAndClick(launchPartyConfirmationButton, robot);
                }
            }

        } catch (ImageNotMatchedException e) {


            log.error(e.getMessage());
            //Go back to main screen
            if (!e.getInMainMap()) {
                goBackToMainMap(robot);
            }

        }
        //Back to main screen
        Thread.sleep(generalConfig.getActionIntervalMs());

        log.info("Done with farmArmies");

    }

    private String handleRange(String nodeLvl) {

        String randomArmyLevel = nodeLvl;

        if (nodeLvl.contains(",")) {
            String[] possibleValues = nodeLvl.split(",");
            randomArmyLevel = possibleValues[random.nextInt(possibleValues.length)];
        }

        if (nodeLvl.contains("-")) {
            String[] bounds = nodeLvl.split("-", 2);
            int step = 1;

            if (Integer.parseInt(bounds[0]) >= 10) {
                step = 5;
            }

            java.util.List<String> possibleValues = new ArrayList<>(5);
            for (int i = Integer.parseInt(bounds[0]); i <= Integer.parseInt(bounds[1]); i = i + step) {
                possibleValues.add(Integer.toString(i));
            }

            randomArmyLevel = possibleValues.get(random.nextInt(possibleValues.size()));
        }

        log.info("Searching for army level {}", randomArmyLevel);
        return randomArmyLevel;

    }

    public void challengeStats(WinUtils.WindowInfo windowInfo, WebClient discordWebClient, ChallengeViewButtons challengeViewButtons, Robot robot) throws IOException, AWTException, URISyntaxException, InterruptedException, TesseractException {
        // Acquire global lock - only one window can control mouse at a time
        synchronized (globalAutomationLock) {
            challengeStatsInternal(windowInfo, discordWebClient, challengeViewButtons, robot);
        }
    }

    private void challengeStatsInternal(WinUtils.WindowInfo windowInfo, WebClient discordWebClient, ChallengeViewButtons challengeViewButtons, Robot robot) throws IOException, AWTException, URISyntaxException, InterruptedException, TesseractException {
        WinUtils.focusWindow(windowInfo.getTitle());
        Thread.sleep(200);

        try {
            robot.keyPress(KeyEvent.VK_D);
            robot.keyRelease(KeyEvent.VK_D);
            Thread.sleep(generalConfig.getActionIntervalMs());

            String challengePage = takeScreenCapture(windowInfo, robot);
            Mat locationSelectionScreen = Imgcodecs.imread(challengePage, CONVERT_IMG_FLAG);
            Double[] pastChallengeCoords = findCoordsOnScreenFlexible(ChallengeViewButtons.PAST_CHALLENGE_TAB_FR.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            moveAndClick(pastChallengeCoords, robot);

            String pastChallengePage = takeScreenCapture(windowInfo, robot);
            Mat pastChallengePageMat = Imgcodecs.imread(pastChallengePage, CONVERT_IMG_FLAG);
            Mat pastChallengeCurrentPageMat = pastChallengePageMat;

            Double[] bottomCoords = findWindowBottomCoords(windowInfo);
            robot.mouseMove(bottomCoords[0].intValue(), bottomCoords[1].intValue());
            Thread.sleep(generalConfig.getActionIntervalMs());

            int mainScrollCounter = 0;
            boolean prevNotFound;
            do {


                pastChallengePageMat = pastChallengeCurrentPageMat;


                try {
                    Double[] coords = findCoordsOnScreenFlexible(challengeViewButtons.getImgPath(), pastChallengePageMat, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    moveAndClick(coords, robot);

                    MultipartBodyBuilder discordRestbuilder = new MultipartBodyBuilder();


                    String challengeDetailsScreenCapturePath = takeScreenCapture(windowInfo, robot);

                    int scrollCounter = 0;
                    discordRestbuilder.part("files[" + scrollCounter + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + ".jpg"));

                    StringBuilder contextText = new StringBuilder("Stats for challenge:\n");

                    Mat challengeDetailsScreenCapture = Imgcodecs.imread(challengeDetailsScreenCapturePath, CONVERT_IMG_FLAG);
                    coords = findCoordsOnScreenFlexible(ChallengeViewButtons.PAST_CHALLENGE_CONTRIBS_BTTN_FR.getImgPath(), challengeDetailsScreenCapture, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    moveAndClick(coords, robot);
                    String challengeScorersScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter, robot);
                    discordRestbuilder.part("files[" + scrollCounter + 1 + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + "scores" + scrollCounter + ".jpg"));
                    Mat challengeScorersScreenCapture = Imgcodecs.imread(challengeDetailsScreenCapturePath, CONVERT_IMG_FLAG);
                    Mat challengeScorersCurrentScreenCapture = challengeScorersScreenCapture;

                    do {

                        bottomCoords = findWindowBottomCoords(windowInfo);
                        robot.mouseMove(bottomCoords[0].intValue(), bottomCoords[1].intValue());
                        Thread.sleep(generalConfig.getActionIntervalMs());

                        challengeScorersScreenCapture = challengeScorersCurrentScreenCapture;

                        contextText.append(ScreenUtils.extractTextFromImage(challengeScorersScreenCapturePath, ocrEngine));

                        robot.mouseWheel(3);
                        Thread.sleep(generalConfig.getActionIntervalMs());

                        scrollCounter++;
                        String challengeScorersCurrentScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter, robot);
                        discordRestbuilder.part("files[" + scrollCounter + 1 + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + "scores" + scrollCounter + ".jpg"));
                        challengeScorersCurrentScreenCapture = Imgcodecs.imread(challengeScorersCurrentScreenCapturePath, CONVERT_IMG_FLAG);
                        challengeScorersScreenCapturePath = challengeScorersCurrentScreenCapturePath;
                    }
                    while (scrollCounter < 3);

                    contextText.append(ScreenUtils.extractTextFromImage(challengeScorersScreenCapturePath, ocrEngine));
                    discordRestbuilder.part("content", contextText.toString());

                    publishContentOnDiscord(discordWebClient, discordRestbuilder);


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

                proceedScrolling(mainScrollCounter, prevNotFound, robot);

                String pastChallengeCurrentPage = takeScreenCapture(windowInfo, robot);
                pastChallengeCurrentPageMat = Imgcodecs.imread(pastChallengeCurrentPage, CONVERT_IMG_FLAG);
            }
            while (mainScrollCounter < SCROLL_AMOUNT);

            //Get back to map screen
            robot.keyPress(VK_ESCAPE);
            robot.keyRelease(VK_ESCAPE);
            Thread.sleep(generalConfig.getActionIntervalMs());

        } catch (ImageNotMatchedException e) {
            log.error("Error in challengeStats: {}", e.getMessage());
            if (!e.getInMainMap()) {
                goBackToMainMap(robot);
            }
        }
    }

    private void publishContentOnDiscord(WebClient discordWebClient, MultipartBodyBuilder discordRestbuilder) {
        if (!generalConfig.isPostDryRun()) {
            log.info("Publish on discord");
            try {
                discordWebClient.post().uri("/messages").body(BodyInserters.fromMultipartData(discordRestbuilder.build())).retrieve().bodyToMono(String.class).block();

            } catch (WebClientException e) {
                log.error("Error calling discord api: {}", e.getMessage());
            }
        }
    }

    private void proceedScrolling(int mainScrollCounter, boolean prevNotFound, Robot robot) throws InterruptedException {
        if (prevNotFound) {
            robot.mouseWheel(1);
            Thread.sleep(generalConfig.getActionIntervalMs());
        } else {
            log.info("Scrolling {} times", mainScrollCounter);
            for (int i = 0; i < mainScrollCounter; i++) {
                robot.mouseWheel(1);
                Thread.sleep(generalConfig.getActionIntervalMs());
            }
        }
    }

    public void receivedRss(WinUtils.WindowInfo windowInfo, WebClient discordWebClient, Robot robot) throws InterruptedException, IOException, AWTException, URISyntaxException, TesseractException {
        // Acquire global lock - only one window can control mouse at a time
        synchronized (globalAutomationLock) {
            receivedRssInternal(windowInfo, discordWebClient, robot);
        }
    }

    private void receivedRssInternal(WinUtils.WindowInfo windowInfo, WebClient discordWebClient, Robot robot) throws InterruptedException, IOException, AWTException, URISyntaxException, TesseractException {
        WinUtils.focusWindow(windowInfo.getTitle());
        Thread.sleep(200);

        try {
            Map<MainMapButtons, Double[]> windowCoords = mainMapButtonsCoordsMap.get(windowInfo.getTitle());
            if (windowCoords == null) {
                throw new IllegalStateException("No coordinates found for window: " + windowInfo.getTitle());
            }
            moveAndClick(windowCoords.get(MainMapButtons.REPORTS), robot);
            Thread.sleep(generalConfig.getActionIntervalMs());

            String repsPage = takeScreenCapture(windowInfo, robot);
            Mat repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            moveAndClick(findCoordsOnScreenFlexible(ReportViewButtons.MARCH_REPORTS_TAB_FR.getImgPath(), repsPageMat, windowInfo, false, generalConfig.getImageQualityLowerBound()), robot);
            Thread.sleep(generalConfig.getActionIntervalMs());

            repsPage = takeScreenCapture(windowInfo, robot);
            repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            int mainScrollCounter = 0;
            boolean prevNotFound;
            do {

                try {
                    Double[] rssReceivedCoords = findCoordsOnScreenFlexible(ReportViewButtons.RSS_RECEIVED_FR.getImgPath(), repsPageMat, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    MultipartBodyBuilder discordRestbuilder = new MultipartBodyBuilder();
                    //Get an image to collect donor

                    //Do stuff
                    WinUtils.RECT rect = new WinUtils.RECT();
                    rect.top = rssReceivedCoords[1].intValue() - 15;
                    rect.bottom = rect.top + 50;
                    rect.left = rssReceivedCoords[0].intValue() - 60;
                    rect.right = rect.left + 250;

                    WinUtils.WindowInfo myCustomWindow = new WinUtils.WindowInfo(rect, "custom");

                    String donationWithDonorCapturePath = takeScreenCapture(myCustomWindow, robot);

                    String donor = ScreenUtils.extractTextFromImage(donationWithDonorCapturePath, ocrEngine);
                    String[] segs = donor.split("\n");

                    StringBuffer contextText = new StringBuffer(segs[segs.length - 1]);


                    moveAndClick(rssReceivedCoords, robot);
                    Thread.sleep(generalConfig.getActionIntervalMs());

                    String amountProvided = ScreenUtils.extractTextFromImage(takeScreenCapture(windowInfo, robot), ocrEngine);
                    segs = amountProvided.split("\n");
                    contextText.append("\n").append(segs[segs.length - 1]);

                    log.info("Text extracted: {}", contextText);
                    discordRestbuilder.part("content", contextText.toString());

                    robot.keyPress(VK_ESCAPE);
                    robot.keyRelease(VK_ESCAPE);
                    Thread.sleep(generalConfig.getActionIntervalMs());
                    prevNotFound = false;

                    publishContentOnDiscord(discordWebClient, discordRestbuilder);


                } catch (ImageNotMatchedException imageNotMatchedException) {
                    //continue
                    prevNotFound = true;

                }

                mainScrollCounter++;
                proceedScrolling(mainScrollCounter, prevNotFound, robot);

                repsPage = takeScreenCapture(windowInfo, robot);
                repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            }
            while (mainScrollCounter < SCROLL_AMOUNT);

            //Get back to map screen
            robot.keyPress(VK_ESCAPE);
            robot.keyRelease(VK_ESCAPE);
            Thread.sleep(generalConfig.getActionIntervalMs());

        } catch (ImageNotMatchedException e) {
            log.error("Error in receivedRss: {}", e.getMessage());
            if (!e.getInMainMap()) {
                goBackToMainMap(robot);
            }
        }
    }


    private void handleStartLocationScreen(WinUtils.WindowInfo windowInfo, Robot robot) throws AWTException, IOException, URISyntaxException, ImageNotMatchedException, InterruptedException {
        String locationSelectionPath = takeScreenCapture(windowInfo, robot);
        Mat locationSelectionScreen = Imgcodecs.imread(locationSelectionPath, CONVERT_IMG_FLAG);
        Double[] fortressIcon = findCoordsOnScreenFlexible(ExpeditionViewButtons.FORTRESS_SELECTION_ICON.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
        moveAndClick(fortressIcon, robot);
        Double[] nextBtnCoords = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.NEXT_BUTTON_EN.getImgPath() : ExpeditionViewButtons.NEXT_BUTTON_FR.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
        moveAndClick(nextBtnCoords, robot);
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

    private void moveAndClick(Double[] coords, Robot robot) throws InterruptedException {
        robot.mouseMove(coords[0].intValue(), coords[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);
        Thread.sleep(generalConfig.getActionIntervalMs());
    }


    private void goBackToMainMap(Robot robot) throws InterruptedException {
        log.info("Attempting to return to main map screen");
        robot.keyPress(VK_ESCAPE);
        robot.keyRelease(VK_ESCAPE);
        Thread.sleep(generalConfig.getActionIntervalMs());
        log.info("Returned to main map screen");
    }


    /**
     * Find a scrollable resource icon (warpstone, relic, etc.) that requires scrolling
     * in the resource list since it's not immediately visible on screen.
     */
    private Double[] findScrollableRssIconWithScrolling(RssType rssType, Mat searchScreen, WinUtils.WindowInfo windowInfo, Double[] expanderCoords, Robot robot) throws InterruptedException, AWTException, IOException, URISyntaxException, ImageNotMatchedException {
        String iconPath = SearchViewButtons.getEnumFromRssType(rssType).getImgPath();
        log.info("Looking for {} icon with scrolling logic, scrolling immediately", rssType);

        // Position mouse in the lower third of the window, left third of the width (inside the scrollable dropdown)
        Rectangle windowRect = new Rectangle(windowInfo.getRect().left, windowInfo.getRect().top,
                Math.abs(windowInfo.getRect().right - windowInfo.getRect().left),
                Math.abs(windowInfo.getRect().bottom - windowInfo.getRect().top));
        double x = windowRect.getX() + windowRect.getWidth() / 6.0;
        double y = windowRect.getY() + windowRect.getHeight() * 5.0 / 6.0;
        Double[] scrollPos = new Double[]{x, y};
        return performScrollToFindIcon(iconPath, rssType.name(), windowInfo, scrollPos, 1, "down", 8, robot);
    }


    private Double[] performScrollToFindIcon(String iconPath, String iconName, WinUtils.WindowInfo windowInfo, Double[] scrollPos, int wheelDirection, String directionName, int maxScrolls, Robot robot) throws InterruptedException, IOException, URISyntaxException, ImageNotMatchedException, AWTException {
        log.info("Scrolling {} to find {} (max {} scrolls)", directionName, iconName, maxScrolls);

        robot.mouseMove(scrollPos[0].intValue(), scrollPos[1].intValue());

        for (int scrollCount = 0; scrollCount < maxScrolls; scrollCount++) {
            robot.mouseWheel(wheelDirection);
            Thread.sleep(generalConfig.getActionIntervalMs());

            String searchViewPath = takeScreenCapture(windowInfo, robot);
            Mat currentScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            try {
                Double[] iconCoords = findCoordsOnScreenFlexible(iconPath, currentScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                log.info("{} icon found after {} {} scrolls", iconName, scrollCount + 1, directionName);
                return iconCoords;
            } catch (ImageNotMatchedException e) {
                log.info("{} not found after scroll {} {}, continuing...", iconName, scrollCount + 1, directionName);
            }
        }

        log.error("{} icon not found after {} {} scrolls", iconName, maxScrolls, directionName);
        throw new ImageNotMatchedException(iconName + " icon not found after scrolling " + directionName, false);
    }


}
