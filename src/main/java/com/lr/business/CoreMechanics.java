package com.lr.business;


import com.lr.config.GeneralConfig;
import com.lr.utils.ScreenUtils;
import com.lr.utils.WindowInputService;
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

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

import static com.lr.utils.ScreenUtils.*;
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
    private final WindowInputService windowInputService;
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
    public CoreMechanics(Tesseract ocrEngine, GeneralConfig generalConfig, ResourceLoader resourceLoader,
                         WindowInputService windowInputService) {
        this.ocrEngine = ocrEngine;
        this.generalConfig = generalConfig;
        this.resourceLoader = resourceLoader;
        this.windowInputService = windowInputService;
    }


    /**
     * Find and farm resources. No global lock needed - uses focus-independent PostMessage.
     */
    public void findAndFarm(String rssLevel, RssType rssType, WinUtils.WindowInfo windowInfo, boolean hasEncampment) throws IOException, URISyntaxException {
        Map<MainMapButtons, Double[]> windowCoords = mainMapButtonsCoordsMap.get(windowInfo.getTitle());
        if (windowCoords == null) {
            throw new IllegalStateException("No coordinates found for window: " + windowInfo.getTitle());
        }
        clickAt(windowInfo, windowCoords.get(MainMapButtons.SEARCH));

        String searchViewPath = takeScreenCapture(windowInfo, windowInputService);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {
            Double[] rssExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            Double[] lvlChoiceExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            clickAt(windowInfo, rssExpander);

            searchViewPath = takeScreenCapture(windowInfo, windowInputService);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            // Special handling for warpstone - requires scrolling to find in resource list
            Double[] rssTypeChoice;
            if (rssType == RssType.WARPSTONE) {
                rssTypeChoice = findWarpstoneIconWithScrolling(searchScreen, windowInfo);
            } else {
                rssTypeChoice = findCoordsOnScreenFlexible(SearchViewButtons.getEnumFromRssType(rssType).getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            }

            log.info("Coords for rss expander for rss {} found at: {}", rssType, rssTypeChoice);
            clickAt(windowInfo, rssTypeChoice);

            clickAt(windowInfo, lvlChoiceExpander);

            searchViewPath = takeScreenCapture(windowInfo, windowInputService);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            rssLevel = handleRange(rssLevel);

            Double[] lvlChoice = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getLevelIconImgPath(rssLevel, generalConfig.getGameLanguage()), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, lvlChoice);

            Double[] searchOnMapCoords = findCoordsOnScreenFlexible(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_FR.getImgPath() : SearchViewButtons.SEARCH_MAP_EN.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, searchOnMapCoords);

            String searchResultsPath = takeScreenCapture(windowInfo, windowInputService);
            Mat searchResultsScreen = Imgcodecs.imread(searchResultsPath, CONVERT_IMG_FLAG);

            Double[] goCoords = findCoordsOnScreenFlexible(Locale.FRENCH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_FR.getImgPath() : SearchViewButtons.GO_RSS_EN.getImgPath(), searchResultsScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, goCoords);

            // Now on map
            Double[] rssSource = findWindowCenterCoords(windowInfo);
            clickAt(windowInfo, rssSource);

            String mapPath = takeScreenCapture(windowInfo, windowInputService);
            Mat mapScreen = Imgcodecs.imread(mapPath, CONVERT_IMG_FLAG);

            Double[] rssCollectSource = findCoordsOnScreenFlexible(SearchViewButtons.getEnumFromRssType(rssType).getOnMapCollectButtonPath(), mapScreen, windowInfo, true, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, rssCollectSource);

            // Now on army selector view
            if (hasEncampment) {
                handleStartLocationScreen(windowInfo);
            }

            String armySelectionPath = takeScreenCapture(windowInfo, windowInputService);
            Mat armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] armyPresetCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.PRESET_ICON.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            Double[] qtyLeftCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.RSS_LEFT.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            useHeroIfLowQtyNode(qtyLeftCoords, windowInfo, armySelectionScreen);

            clickAt(windowInfo, armyPresetCoords);

            String armyPresetsPath = takeScreenCapture(windowInfo, windowInputService);
            Mat armyPresetsScreen = Imgcodecs.imread(armyPresetsPath, CONVERT_IMG_FLAG);
            Double[] armyPresetGatheringCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.PRESET_RADIO.getImgPath(), armyPresetsScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, armyPresetGatheringCoords);

            armySelectionPath = takeScreenCapture(windowInfo, windowInputService);
            armySelectionScreen = Imgcodecs.imread(armySelectionPath, CONVERT_IMG_FLAG);

            Double[] launchCoords = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, launchCoords);
        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());
            if (!e.getInMainMap()) {
                goBackToMainMap(windowInfo);
            }
        }
        windowInputService.delay(generalConfig.getActionIntervalMs());
        log.info("Done with findAndFarm");
    }

    private void useHeroIfLowQtyNode(Double[] coords, WinUtils.WindowInfo windowInfo, Mat armySelectionScreen) throws IOException, URISyntaxException {
        try {
            // Note: OCR extraction for quantity check - we'll capture the full window and crop in post-processing
            String qtyPath = takeScreenCapture(windowInfo, "qtyExtract", windowInputService);
            ocrEngine.setPageSegMode(7);
            String extractedText = ScreenUtils.extractTextFromImage(qtyPath, ocrEngine);
            log.info("Extracted Text: {}", extractedText);

            String[] splitText = extractedText.trim().split("/");
            String qtyAvail = splitText.length > 1 ? splitText[1].trim() : "";
            log.info("Extracted residual qty: {}", qtyAvail);

            if (qtyAvail.matches("^[0-9]+$") || (qtyAvail.contains("k") && Double.parseDouble(extractSafelyNumberFromOCRString(qtyAvail).replaceAll(",", ".")) < 30)) {
                Double[] heroSliderCoords = findCoordsOnScreenFlexible(ExpeditionViewButtons.HERO_SLIDER.getImgPath(), armySelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                clickAt(windowInfo, heroSliderCoords);
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

    /**
     * Farm armies. No global lock needed - uses focus-independent PostMessage.
     */
    public void armyFarming(String armyLvl, int armyPreset, WinUtils.WindowInfo windowInfo, boolean hasEncampment, Boolean isSkelly, Boolean isFirstRun) throws IOException, URISyntaxException {
        Map<MainMapButtons, Double[]> windowCoords = mainMapButtonsCoordsMap.get(windowInfo.getTitle());
        if (windowCoords == null) {
            throw new IllegalStateException("No coordinates found for window: " + windowInfo.getTitle());
        }
        clickAt(windowInfo, windowCoords.get(MainMapButtons.SEARCH));
        String searchViewPath = takeScreenCapture(windowInfo, windowInputService);
        Mat searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

        try {
            Double[] mapSearchButton = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.SEARCH_MAP_EN.getImgPath() : SearchViewButtons.SEARCH_MAP_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            if (isFirstRun) {
                Double[] rssExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                clickAt(windowInfo, rssExpander);
                searchViewPath = takeScreenCapture(windowInfo, windowInputService);
                searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

                Double[] lvlChoiceExpander = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

                Double[] armyChoice = findCoordsOnScreenFlexible(SearchViewButtons.ARMY_ICON.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                clickAt(windowInfo, armyChoice);

                clickAt(windowInfo, lvlChoiceExpander);

                searchViewPath = takeScreenCapture(windowInfo, windowInputService);
                searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

                armyLvl = handleRange(armyLvl);

                Double[] lvlChoice = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_LEVEL_EXPANDER.getLevelIconImgPath(armyLvl, generalConfig.getGameLanguage()), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                clickAt(windowInfo, lvlChoice);
            }

            clickAt(windowInfo, mapSearchButton);

            searchViewPath = takeScreenCapture(windowInfo, windowInputService);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            if (isSkelly != null && isSkelly) {
                Double[] skellyCoord = findCoordsOnScreenFlexible(SearchViewButtons.SEARCH_ARMY_SKELLY.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                Double[] extrapolateGoIconCoords = computeGoIconForSpecificArmy(skellyCoord, windowInfo, Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_EN.getImgPath() : SearchViewButtons.GO_RSS_FR.getImgPath(), searchScreen);
                log.info("Computed coord for go button on skelly army row: {}x{}", extrapolateGoIconCoords[0], extrapolateGoIconCoords[1]);
                clickAt(windowInfo, extrapolateGoIconCoords);
            } else {
                Double[] goToArmy = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? SearchViewButtons.GO_RSS_EN.getImgPath() : SearchViewButtons.GO_RSS_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                clickAt(windowInfo, goToArmy);
            }

            Double[] armyOnMap = findWindowCenterCoords(windowInfo);
            clickAt(windowInfo, armyOnMap);

            searchViewPath = takeScreenCapture(windowInfo, windowInputService);
            searchScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            Double[] attackBtn = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, true, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, attackBtn);

            if (hasEncampment) {
                handleStartLocationScreen(windowInfo);
            }

            String armySelectionViewPath = takeScreenCapture(windowInfo, windowInputService);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);
            log.info("Clicking army preset #{}", armyPreset);
            Double[] armyPresetBtn = findCoordsOnScreenFlexible(ExpeditionViewButtons.getPresetById(armyPreset).getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            clickAt(windowInfo, armyPresetBtn);

            armySelectionViewPath = takeScreenCapture(windowInfo, windowInputService);
            searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);

            try {
                Double[] launchPartyButton = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_EN.getImgPath() : ExpeditionViewButtons.LAUNCH_EXPEDITION_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                clickAt(windowInfo, launchPartyButton);
            } catch (ImageNotMatchedException e) {
                // Use case for big army warning
                if (!"ALL".equalsIgnoreCase(armyLvl) && Integer.parseInt(armyLvl) >= FAT_ARMY_THRESHOLD) {
                    armySelectionViewPath = takeScreenCapture(windowInfo, windowInputService);
                    searchScreen = Imgcodecs.imread(armySelectionViewPath, CONVERT_IMG_FLAG);

                    Double[] launchPartyConfirmationButton = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_EN.getImgPath() : ExpeditionViewButtons.CONFIRM_ATTACK_BUTTON_FR.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    clickAt(windowInfo, launchPartyConfirmationButton);
                }
            }

        } catch (ImageNotMatchedException e) {
            log.error(e.getMessage());
            if (!e.getInMainMap()) {
                goBackToMainMap(windowInfo);
            }
        }
        windowInputService.delay(generalConfig.getActionIntervalMs());
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

    /**
     * Challenge stats. No global lock needed - uses focus-independent PostMessage.
     */
    public void challengeStats(WinUtils.WindowInfo windowInfo, WebClient discordWebClient, ChallengeViewButtons challengeViewButtons) throws IOException, URISyntaxException, TesseractException {
        try {
            pressKey(windowInfo, KeyEvent.VK_D);
            windowInputService.delay(generalConfig.getActionIntervalMs());

            String challengePage = takeScreenCapture(windowInfo, windowInputService);
            Mat locationSelectionScreen = Imgcodecs.imread(challengePage, CONVERT_IMG_FLAG);
            Double[] pastChallengeCoords = findCoordsOnScreenFlexible(ChallengeViewButtons.PAST_CHALLENGE_TAB_FR.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());

            clickAt(windowInfo, pastChallengeCoords);

            String pastChallengePage = takeScreenCapture(windowInfo, windowInputService);
            Mat pastChallengePageMat = Imgcodecs.imread(pastChallengePage, CONVERT_IMG_FLAG);
            Mat pastChallengeCurrentPageMat = pastChallengePageMat;

            int mainScrollCounter = 0;
            boolean prevNotFound;
            do {
                pastChallengePageMat = pastChallengeCurrentPageMat;

                try {
                    Double[] coords = findCoordsOnScreenFlexible(challengeViewButtons.getImgPath(), pastChallengePageMat, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    clickAt(windowInfo, coords);

                    MultipartBodyBuilder discordRestbuilder = new MultipartBodyBuilder();

                    String challengeDetailsScreenCapturePath = takeScreenCapture(windowInfo, windowInputService);

                    int scrollCounter = 0;
                    discordRestbuilder.part("files[" + scrollCounter + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + ".jpg"));

                    StringBuilder contextText = new StringBuilder("Stats for challenge:\n");

                    Mat challengeDetailsScreenCapture = Imgcodecs.imread(challengeDetailsScreenCapturePath, CONVERT_IMG_FLAG);
                    coords = findCoordsOnScreenFlexible(ChallengeViewButtons.PAST_CHALLENGE_CONTRIBS_BTTN_FR.getImgPath(), challengeDetailsScreenCapture, windowInfo, false, generalConfig.getImageQualityLowerBound());
                    clickAt(windowInfo, coords);
                    String challengeScorersScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter, windowInputService);
                    discordRestbuilder.part("files[" + scrollCounter + 1 + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + "scores" + scrollCounter + ".jpg"));
                    Mat challengeScorersCurrentScreenCapture = Imgcodecs.imread(challengeDetailsScreenCapturePath, CONVERT_IMG_FLAG);

                    do {
                        contextText.append(ScreenUtils.extractTextFromImage(challengeScorersScreenCapturePath, ocrEngine));

                        scroll(windowInfo, 3);
                        windowInputService.delay(generalConfig.getActionIntervalMs());

                        scrollCounter++;
                        String challengeScorersCurrentScreenCapturePath = takeScreenCapture(windowInfo, "scores" + scrollCounter, windowInputService);
                        discordRestbuilder.part("files[" + scrollCounter + 1 + "]", new FileSystemResource("tmp" + windowInfo.getTitle() + "scores" + scrollCounter + ".jpg"));
                        challengeScorersCurrentScreenCapture = Imgcodecs.imread(challengeScorersCurrentScreenCapturePath, CONVERT_IMG_FLAG);
                        challengeScorersScreenCapturePath = challengeScorersCurrentScreenCapturePath;
                    }
                    while (scrollCounter < 3);

                    contextText.append(ScreenUtils.extractTextFromImage(challengeScorersScreenCapturePath, ocrEngine));
                    discordRestbuilder.part("content", contextText.toString());

                    publishContentOnDiscord(discordWebClient, discordRestbuilder);

                    pressKey(windowInfo, VK_ESCAPE);
                    windowInputService.delay(generalConfig.getActionIntervalMs());

                    pressKey(windowInfo, VK_ESCAPE);
                    windowInputService.delay(generalConfig.getActionIntervalMs());
                    prevNotFound = false;

                } catch (ImageNotMatchedException e) {
                    log.info("Challenge not found move fwd");
                    prevNotFound = true;
                }

                mainScrollCounter++;
                log.info("Arrived at {} scrolls", mainScrollCounter);

                proceedScrolling(windowInfo, mainScrollCounter, prevNotFound);

                String pastChallengeCurrentPage = takeScreenCapture(windowInfo, windowInputService);
                pastChallengeCurrentPageMat = Imgcodecs.imread(pastChallengeCurrentPage, CONVERT_IMG_FLAG);
            }
            while (mainScrollCounter < SCROLL_AMOUNT);

            pressKey(windowInfo, VK_ESCAPE);
            windowInputService.delay(generalConfig.getActionIntervalMs());

        } catch (ImageNotMatchedException e) {
            log.error("Error in challengeStats: {}", e.getMessage());
            if (!e.getInMainMap()) {
                goBackToMainMap(windowInfo);
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

    // ============ Helper methods for focus-independent input ============

    /**
     * Click at absolute screen coordinates using PostMessage (focus-independent).
     */
    private void clickAt(WinUtils.WindowInfo windowInfo, Double[] coords) {
        int[] client = windowInputService.screenToClient(windowInfo.getHwnd(),
                coords[0].intValue(), coords[1].intValue());
        windowInputService.leftClick(windowInfo.getHwnd(), client[0], client[1]);
        windowInputService.delay(generalConfig.getActionIntervalMs());
    }

    /**
     * Press a key using PostMessage (focus-independent).
     */
    private void pressKey(WinUtils.WindowInfo windowInfo, int virtualKeyCode) {
        windowInputService.keyTap(windowInfo.getHwnd(), virtualKeyCode);
    }

    /**
     * Scroll the mouse wheel using PostMessage (focus-independent).
     */
    private void scroll(WinUtils.WindowInfo windowInfo, int delta) {
        Double[] center = findWindowCenterCoords(windowInfo);
        int[] client = windowInputService.screenToClient(windowInfo.getHwnd(),
                center[0].intValue(), center[1].intValue());
        windowInputService.mouseWheel(windowInfo.getHwnd(), client[0], client[1], delta);
    }

    private void proceedScrolling(WinUtils.WindowInfo windowInfo, int mainScrollCounter, boolean prevNotFound) {
        if (prevNotFound) {
            scroll(windowInfo, 1);
            windowInputService.delay(generalConfig.getActionIntervalMs());
        } else {
            log.info("Scrolling {} times", mainScrollCounter);
            for (int i = 0; i < mainScrollCounter; i++) {
                scroll(windowInfo, 1);
                windowInputService.delay(generalConfig.getActionIntervalMs());
            }
        }
    }

    /**
     * Received RSS stats. No global lock needed - uses focus-independent PostMessage.
     */
    public void receivedRss(WinUtils.WindowInfo windowInfo, WebClient discordWebClient) throws IOException, URISyntaxException, TesseractException {
        try {
            Map<MainMapButtons, Double[]> windowCoords = mainMapButtonsCoordsMap.get(windowInfo.getTitle());
            if (windowCoords == null) {
                throw new IllegalStateException("No coordinates found for window: " + windowInfo.getTitle());
            }
            clickAt(windowInfo, windowCoords.get(MainMapButtons.REPORTS));

            String repsPage = takeScreenCapture(windowInfo, windowInputService);
            Mat repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            clickAt(windowInfo, findCoordsOnScreenFlexible(ReportViewButtons.MARCH_REPORTS_TAB_FR.getImgPath(), repsPageMat, windowInfo, false, generalConfig.getImageQualityLowerBound()));

            repsPage = takeScreenCapture(windowInfo, windowInputService);
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

                    String donationWithDonorCapturePath = takeScreenCapture(myCustomWindow, windowInputService);

                    String donor = ScreenUtils.extractTextFromImage(donationWithDonorCapturePath, ocrEngine);
                    String[] segs = donor.split("\n");

                    StringBuilder contextText = new StringBuilder(segs[segs.length - 1]);

                    clickAt(windowInfo, rssReceivedCoords);

                    String amountProvided = ScreenUtils.extractTextFromImage(takeScreenCapture(windowInfo, windowInputService), ocrEngine);
                    segs = amountProvided.split("\n");
                    contextText.append("\n").append(segs[segs.length - 1]);

                    log.info("Text extracted: {}", contextText);
                    discordRestbuilder.part("content", contextText.toString());

                    pressKey(windowInfo, VK_ESCAPE);
                    windowInputService.delay(generalConfig.getActionIntervalMs());
                    prevNotFound = false;

                    publishContentOnDiscord(discordWebClient, discordRestbuilder);


                } catch (ImageNotMatchedException imageNotMatchedException) {
                    //continue
                    prevNotFound = true;

                }

                mainScrollCounter++;
                proceedScrolling(windowInfo, mainScrollCounter, prevNotFound);

                repsPage = takeScreenCapture(windowInfo, windowInputService);
                repsPageMat = Imgcodecs.imread(repsPage, CONVERT_IMG_FLAG);

            }
            while (mainScrollCounter < SCROLL_AMOUNT);

            //Get back to map screen
            pressKey(windowInfo, VK_ESCAPE);
            windowInputService.delay(generalConfig.getActionIntervalMs());

        } catch (ImageNotMatchedException e) {
            log.error("Error in receivedRss: {}", e.getMessage());
            // Go back to main screen when image recognition fails
            if (!e.getInMainMap()) {
                goBackToMainMap(windowInfo);
            }
        }
    }


    private void handleStartLocationScreen(WinUtils.WindowInfo windowInfo) throws IOException, URISyntaxException, ImageNotMatchedException {
        String locationSelectionPath = takeScreenCapture(windowInfo, windowInputService);
        Mat locationSelectionScreen = Imgcodecs.imread(locationSelectionPath, CONVERT_IMG_FLAG);
        Double[] fortressIcon = findCoordsOnScreenFlexible(ExpeditionViewButtons.FORTRESS_SELECTION_ICON.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
        clickAt(windowInfo, fortressIcon);
        Double[] nextBtnCoords = findCoordsOnScreenFlexible(Locale.ENGLISH.equals(generalConfig.getGameLanguage()) ? ExpeditionViewButtons.NEXT_BUTTON_EN.getImgPath() : ExpeditionViewButtons.NEXT_BUTTON_FR.getImgPath(), locationSelectionScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
        clickAt(windowInfo, nextBtnCoords);
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

    /**
     * Go back to main map by pressing ESC multiple times.
     * Uses focus-independent PostMessage.
     */
    private void goBackToMainMap(WinUtils.WindowInfo windowInfo) {
        log.info("Attempting to return to main map screen");

        // Press ESC multiple times to ensure we get back to main map from any nested screen
        for (int i = 0; i < 3; i++) {
            pressKey(windowInfo, VK_ESCAPE);
            windowInputService.delay(500); // Short delay between key presses
        }

        // Longer delay to allow UI transitions to complete
        windowInputService.delay(generalConfig.getActionIntervalMs() * 2);

        log.info("Returned to main map screen");
    }


    /**
     * Find warpstone icon with scrolling logic since it's not immediately visible on screen.
     * Uses focus-independent PostMessage.
     */
    private Double[] findWarpstoneIconWithScrolling(Mat searchScreen, WinUtils.WindowInfo windowInfo) throws IOException, URISyntaxException, ImageNotMatchedException {
        log.info("Looking for warpstone icon with scrolling logic (primarily downward)");

        // First, try to find warpstone without scrolling
        try {
            Double[] warpstoneIconCoords = findCoordsOnScreenFlexible(SearchViewButtons.WARPSTONE_ICON.getImgPath(), searchScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
            log.info("Warpstone icon found without scrolling");
            return warpstoneIconCoords;
        } catch (ImageNotMatchedException e) {
            log.info("Warpstone icon not visible, starting scrolling search...");
        }

        // Click on resource list area to ensure it's focused for scrolling
        Double[] resourceListCoords = findResourceListScrollPosition(windowInfo);
        clickAt(windowInfo, resourceListCoords);

        // Scroll down to find warpstone (primary direction based on user feedback)
        return performWarpstoneScroll(windowInfo, 1, "down", 8);
    }

    /**
     * Calculate the optimal mouse position for scrolling the resource list
     */
    private Double[] findResourceListScrollPosition(WinUtils.WindowInfo windowInfo) {
        Rectangle windowRect = new Rectangle(windowInfo.getRect().left, windowInfo.getRect().top,
                Math.abs(windowInfo.getRect().right - windowInfo.getRect().left),
                Math.abs(windowInfo.getRect().bottom - windowInfo.getRect().top));

        // Position mouse on the left side of the window where resource icons are typically located
        // About 1/4 from the left and middle height
        double x = windowRect.getX() + windowRect.getWidth() * 0.25;
        double y = windowRect.getY() + windowRect.getHeight() * 0.5;

        return new Double[]{x, y};
    }

    /**
     * Perform focused scrolling to find warpstone icon.
     * Uses focus-independent PostMessage.
     */
    private Double[] performWarpstoneScroll(WinUtils.WindowInfo windowInfo, int wheelDirection, String directionName, int maxScrolls) throws IOException, URISyntaxException, ImageNotMatchedException {
        log.info("Scrolling {} to find warpstone (max {} scrolls)", directionName, maxScrolls);

        for (int scrollCount = 0; scrollCount < maxScrolls; scrollCount++) {
            // Perform scroll using PostMessage
            scroll(windowInfo, wheelDirection);
            windowInputService.delay(generalConfig.getActionIntervalMs());

            // Take screenshot after scroll
            String searchViewPath = takeScreenCapture(windowInfo, windowInputService);
            Mat currentScreen = Imgcodecs.imread(searchViewPath, CONVERT_IMG_FLAG);

            // Try to find warpstone
            try {
                Double[] warpstoneIconCoords = findCoordsOnScreenFlexible(SearchViewButtons.WARPSTONE_ICON.getImgPath(), currentScreen, windowInfo, false, generalConfig.getImageQualityLowerBound());
                log.info("Warpstone icon found after {} {} scrolls", scrollCount + 1, directionName);
                return warpstoneIconCoords;
            } catch (ImageNotMatchedException e) {
                log.info("Warpstone not found after scroll {} {}, continuing...", scrollCount + 1, directionName);
            }
        }

        log.error("Warpstone icon not found after {} {} scrolls", maxScrolls, directionName);
        throw new ImageNotMatchedException("Warpstone icon not found after scrolling " + directionName, false);
    }


}
