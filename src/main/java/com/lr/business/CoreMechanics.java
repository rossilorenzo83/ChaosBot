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

import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static com.lr.utils.ScreenUtils.takeScreenCapture;
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

            rssLevel = handleRange(rssLevel);

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
            Double[] qtyLeftCoords = findCoordsOnScreen(ExpeditionViewButtons.RSS_LEFT.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());


            useHeroIfLowQtyNode(qtyLeftCoords, windowInfo, armySelectionScreen);

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

    private void useHeroIfLowQtyNode(Double[] coords, WinUtils.WindowInfo windowInfo, Mat armySelectionScreen) throws AWTException, IOException, URISyntaxException, InterruptedException {
        try {
            Rectangle rect = new Rectangle(coords[0].intValue() + 10, coords[1].intValue() - 10, 100, 20);
            String qtyPath = takeScreenCapture(rect, "qtyExtract", windowInfo.getTitle());
            //Treat input as single line text
            ocrEngine.setPageSegMode(7);
            String extractedText = ScreenUtils.extractTextFromImage(qtyPath, ocrEngine);
            log.info("Extracted Text: {}", extractedText);

            String[] splitText = extractedText.trim().split("/");
            String qtyAvail = splitText.length > 1 ? splitText[1].trim() : "";
            log.info("Extracted residual qty: {}", qtyAvail);

            if (qtyAvail.matches("^[0-9]+$") || (qtyAvail.contains("k") && Double.parseDouble(qtyAvail.split("k")[0].replaceAll(",",".")) < 30)) {
                Double[] heroSliderCoords = findCoordsOnScreen(ExpeditionViewButtons.HERO_SLIDER.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                moveAndClick(heroSliderCoords);
            }

        } catch (ImageNotMatchedException e) {
            log.info("Hero not avail");
        } catch (TesseractException e) {
            log.info("Could not extract qty left on node {}", e.getMessage());
        }
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


            armyLvl = handleRange(armyLvl);

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


            try {

                Double[] launchPartyButton = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                moveAndClick(launchPartyButton);
            } catch (ImageNotMatchedException e) {

                //Use case for big army warning
                if (!"ALL".equalsIgnoreCase(armyLvl) && Integer.parseInt(armyLvl) >= FAT_ARMY_THRESHOLD) {
                    armySelectionViewPath = takeScreenCapture(windowInfo);
                    searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);

                    Double[] launchPartyConfirmationButton = findCoordsOnScreen(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                    moveAndClick(launchPartyConfirmationButton);
                }
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
            boolean prevNotFound;
            do {


                pastChallengePageMat = pastChallengeCurrentPageMat;


                try {
                    Double[] coords = findCoordsOnScreen(challengeViewButtons.getImgPath(), pastChallengePageMat, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    moveAndClick(coords);

                    MultipartBodyBuilder discordRestbuilder = new MultipartBodyBuilder();


                    String challengeDetailsScreenCapturePath = takeScreenCapture(windowInfo);

                    int scrollCounter = 0;
                    discordRestbuilder.part("files[" + scrollCounter + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + ".jpg"));

                    StringBuilder contextText = new StringBuilder("Stats for challenge:\n");

                    Mat challengeDetailsScreenCapture = Imgcodecs.imread(challengeDetailsScreenCapturePath, CONVERT_IMG_FLAG);
                    coords = findCoordsOnScreen(ChallengeViewButtons.PAST_CHALLENGE_CONTRIBS_BTTN_FR.getImgPath(), challengeDetailsScreenCapture, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    moveAndClick(coords);
                    String challengeScorersScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter);
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
                        String challengeScorersCurrentScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter);
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

                proceedScrolling(mainScrollCounter, prevNotFound);

                String pastChallengeCurrentPage = takeScreenCapture(windowInfo);
                pastChallengeCurrentPageMat = Imgcodecs.imread(pastChallengeCurrentPage, CONVERT_IMG_FLAG);
            }
            while (mainScrollCounter < SCROLL_AMOUNT);

            //Get back to map screen
            robot.keyPress(VK_ESCAPE);
            robot.keyRelease(VK_ESCAPE);
            Thread.sleep(generalConfig.getActionIntervalMs());

        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());
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

    private void proceedScrolling(int mainScrollCounter, boolean prevNotFound) throws InterruptedException {
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

    public void receivedRss(WinUtils.WindowInfo windowInfo, WebClient discordWebClient) throws InterruptedException, IOException, AWTException, URISyntaxException, TesseractException {

        try {
            moveAndClick(mainMapButtonsCoordsMap.get(windowInfo.getTitle()).get(MainMapButtons.REPORTS));
            Thread.sleep(generalConfig.getActionIntervalMs());

            String repsPage = takeScreenCapture(windowInfo);
            Mat repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            moveAndClick(findCoordsOnScreen(ReportViewButtons.MARCH_REPORTS_TAB_FR.getImgPath(), repsPageMat, windowInfo, false, generalConfig.getImageQualityLowerBound()));
            Thread.sleep(generalConfig.getActionIntervalMs());

            repsPage = takeScreenCapture(windowInfo);
            repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            int mainScrollCounter = 0;
            boolean prevNotFound;
            do {

                try {
                    Double[] rssReceivedCoords = findCoordsOnScreen(ReportViewButtons.RSS_RECEIVED_FR.getImgPath(), repsPageMat, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    MultipartBodyBuilder discordRestbuilder = new MultipartBodyBuilder();
                    //Get an image to collect donor

                    //Do stuff
                    WinUtils.RECT rect = new WinUtils.RECT();
                    rect.top = rssReceivedCoords[1].intValue() - 15;
                    rect.bottom = rect.top + 50;
                    rect.left = rssReceivedCoords[0].intValue() - 60;
                    rect.right = rect.left + 250;

                    WinUtils.WindowInfo myCustomWindow = new WinUtils.WindowInfo(rect, "custom");

                    String donationWithDonorCapturePath = takeScreenCapture(myCustomWindow);

                    String donor = ScreenUtils.extractTextFromImage(donationWithDonorCapturePath, ocrEngine);
                    String[] segs = donor.split("\n");

                    StringBuffer contextText = new StringBuffer(segs[segs.length - 1]);


                    moveAndClick(rssReceivedCoords);
                    Thread.sleep(generalConfig.getActionIntervalMs());

                    String amountProvided = ScreenUtils.extractTextFromImage(takeScreenCapture(windowInfo), ocrEngine);
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
                proceedScrolling(mainScrollCounter, prevNotFound);

                repsPage = takeScreenCapture(windowInfo);
                repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            }
            while (mainScrollCounter < SCROLL_AMOUNT);

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


    private void goBackToMainMap() throws InterruptedException {
        robot.keyPress(VK_ESCAPE);
        robot.keyRelease(VK_ESCAPE);
        Thread.sleep(generalConfig.getActionIntervalMs());
    }


}
