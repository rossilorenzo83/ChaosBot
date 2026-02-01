package com.lr.business;

import com.lr.config.GeneralConfig;
import com.lr.config.MarchConfig;
import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.web.reactive.function.client.WebClient;

import java.awt.*;
import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.lr.business.CoreMechanics.CONVERT_IMG_FLAG;
import static com.lr.utils.ScreenUtils.*;

/**
 * Worker class that handles automation for a single game window.
 * Each worker owns its own Robot instance to prevent thread interference.
 *
 * This class encapsulates:
 * - Window-specific state (availMarches, firstRun, etc.)
 * - Window-specific Robot instance
 * - Main automation loop for the window
 * - Proper resource cleanup
 */
@Slf4j
public class WindowAutomationWorker implements Runnable {

    private final WinUtils.WindowInfo windowInfo;
    private final CoreMechanics coreMechanics;
    private final GeneralConfig generalConfig;
    private final MarchConfig marchConfig;
    private final WebClient discordWebClient;
    private final Random random;
    private final com.lr.config.Beans.RobotFactory robotFactory;

    // Per-worker state
    private Robot robot;
    private Integer availMarches;
    private Boolean firstRun = true;
    private Long timeLastActionPerformed;
    private volatile boolean shutdownRequested = false;

    /**
     * Creates a new window automation worker.
     * Robot instance will be created lazily on first use.
     */
    public WindowAutomationWorker(
            WinUtils.WindowInfo windowInfo,
            CoreMechanics coreMechanics,
            GeneralConfig generalConfig,
            MarchConfig marchConfig,
            WebClient discordWebClient,
            Random random,
            com.lr.config.Beans.RobotFactory robotFactory) {

        this.windowInfo = windowInfo;
        this.coreMechanics = coreMechanics;
        this.generalConfig = generalConfig;
        this.marchConfig = marchConfig;
        this.discordWebClient = discordWebClient;
        this.random = random;
        this.robotFactory = robotFactory;
        this.availMarches = marchConfig.getMarchesAvailable();
        this.timeLastActionPerformed = System.currentTimeMillis();
    }

    /**
     * Gets or creates the Robot instance for this worker.
     * Lazy initialization to avoid creating Robot in constructor.
     */
    private Robot getRobot() throws AWTException {
        if (robot == null) {
            robot = robotFactory.createRobot();
            log.info("Created Robot instance for window: {}", windowInfo.getTitle());
        }
        return robot;
    }

    @Override
    public void run() {
        log.info("Starting automation worker for window: {}", windowInfo.getTitle());

        try {
            // Initialize window and start main loop
            initializeWindow();
            processWindowLoop();

        } catch (InterruptedException e) {
            log.info("Worker interrupted for window: {}", windowInfo.getTitle());
            Thread.currentThread().interrupt(); // Preserve interrupt status
        } catch (AWTException | IOException | URISyntaxException | TesseractException e) {
            log.error("Fatal error in automation worker for window: {}", windowInfo.getTitle(), e);

        } finally {
            cleanup();
        }
    }

    /**
     * Initialize window coordinates and setup.
     */
    private void initializeWindow() throws AWTException, IOException, URISyntaxException, InterruptedException {
        log.info("Initializing window: {}", windowInfo.getTitle());

        String fullImagePath = takeScreenCapture(windowInfo, getRobot());
        Mat fullScreen = Imgcodecs.imread(fullImagePath, CONVERT_IMG_FLAG);
        log.info("Loaded image dimensions: {}", fullScreen.size().toString());

        Map<MainMapButtons, Double[]> currentWindowCoords = new HashMap<>();
        Boolean hasEncampments = true;

        // Find coordinates for all main map buttons
        for (MainMapButtons mainMapButton : MainMapButtons.values()) {
            log.info("Searching coords for control: {}", mainMapButton.name());
            try {
                Double[] absCoords = findCoordsOnScreenFlexible(
                    mainMapButton.getImgPath(),
                    fullScreen,
                    windowInfo,
                    true,
                    generalConfig.getImageQualityLowerBound()
                );
                currentWindowCoords.put(mainMapButton, absCoords);

            } catch (ImageNotMatchedException e) {
                if (mainMapButton.equals(MainMapButtons.ENCAMPMENTS)) {
                    hasEncampments = false;
                    log.info("No encampments found for window: {}", windowInfo.getTitle());
                } else {
                    log.error("Failed to find coordinates for {}: {}", mainMapButton.name(), e.getMessage());
                }
            }
        }

        // Store coordinates in CoreMechanics (thread-safe)
        registerWindowCoordinates(currentWindowCoords);

        // Store hasEncampments in worker state
        this.hasEncampments = hasEncampments;

        log.info("Window initialization complete for: {}", windowInfo.getTitle());
    }

    private Boolean hasEncampments = true;

    /**
     * Thread-safe registration of window coordinates in CoreMechanics.
     * Uses double-checked locking pattern to safely initialize the ConcurrentHashMap.
     */
    private void registerWindowCoordinates(Map<MainMapButtons, Double[]> currentWindowCoords) {
        ConcurrentMap<String, Map<MainMapButtons, Double[]>> existingCoordsMap =
            coreMechanics.getMainMapButtonsCoordsMap();

        if (existingCoordsMap == null) {
            // Use synchronized initialization with dedicated lock
            synchronized (coreMechanics.getCoordsMapInitLock()) {
                existingCoordsMap = coreMechanics.getMainMapButtonsCoordsMap();
                if (existingCoordsMap == null) {
                    existingCoordsMap = new ConcurrentHashMap<>();
                    coreMechanics.setMainMapButtonsCoordsMap(existingCoordsMap);
                }
            }
            // Reassign from CoreMechanics to ensure we have the correct reference
            // (in case another thread initialized it while we were waiting for the lock)
            existingCoordsMap = coreMechanics.getMainMapButtonsCoordsMap();
        }

        // Now add this window's coordinates (thread-safe due to ConcurrentHashMap)
        existingCoordsMap.put(windowInfo.getTitle(), currentWindowCoords);
    }

    /**
     * Main processing loop for the window.
     * Loop continues until shutdown is requested or thread is interrupted.
     */
    private void processWindowLoop() throws AWTException, IOException, URISyntaxException, InterruptedException, TesseractException {
        log.info("Entering main processing loop for window: {}", windowInfo.getTitle());

        while (!shutdownRequested && !Thread.currentThread().isInterrupted()) {
            // Check if timer expired and reset marches
            if (availMarches == 0 &&
                (System.currentTimeMillis() - timeLastActionPerformed) > (marchConfig.getMarchesIntervalMins() * 60 * 1000)) {
                log.info("Timer expired for window: {}. Resetting marches.", windowInfo.getTitle());
                availMarches = marchConfig.getMarchesAvailable();
                firstRun = true;
            }

            // Process actions if marches available
            if (availMarches > 0) {
                log.info("Processing action for window: {} (marches remaining: {})",
                    windowInfo.getTitle(), availMarches);

                File tmpFolder = LoadLibs.extractTessResources("win32-x86-64");
                log.info("Tesseract tmp folder path: {}", tmpFolder.getPath());
                System.setProperty("java.library.path", tmpFolder.getPath());

                performAction();

                availMarches--;
                firstRun = false;
                timeLastActionPerformed = System.currentTimeMillis();
            }

            // Small sleep to prevent tight loop when no marches available
            if (availMarches == 0) {
                Thread.sleep(1000); // Check every second for timer expiration
            }
        }

        log.info("Processing loop terminated for window: {}", windowInfo.getTitle());
    }

    /**
     * Request graceful shutdown of this worker.
     * This method can be called from external threads to signal shutdown.
     */
    public void requestShutdown() {
        log.info("Shutdown requested for window: {}", windowInfo.getTitle());
        shutdownRequested = true;
    }

    /**
     * Perform the configured action type.
     */
    private void performAction() throws AWTException, IOException, URISyntaxException, InterruptedException, TesseractException {
        switch (generalConfig.getActionType()) {
            case ARMY_FARMING:
                log.info("Executing ARMY_FARMING for window: {}", windowInfo.getTitle());
                int marchPreset = marchConfig.getMarchPreset() != null ?
                    marchConfig.getMarchPreset() : availMarches;

                coreMechanics.armyFarming(
                    marchConfig.getTargetArmyLevel(),
                    marchPreset,
                    windowInfo,
                    hasEncampments,
                    marchConfig.getIsSkelly(),
                    firstRun,
                    getRobot()
                );
                break;

            case CHALLENGE_STATS:
                log.info("Executing CHALLENGE_STATS for window: {}", windowInfo.getTitle());
                List<ChallengeViewButtons> challengeButtons = Arrays.asList(
                    ChallengeViewButtons.PAST_CHALLENGE_ALLIANCE_BANNER_FR,
                    ChallengeViewButtons.PAST_CHALLENGE_HORDE_BANNER_FR,
                    ChallengeViewButtons.PAST_CHALLENGE_LEGION_BANNER_FR
                );

                for (ChallengeViewButtons challengeViewButton : challengeButtons) {
                    coreMechanics.challengeStats(windowInfo, discordWebClient, challengeViewButton, getRobot());
                }
                break;

            case DONORS_STATS:
                log.info("Executing DONORS_STATS for window: {}", windowInfo.getTitle());
                coreMechanics.receivedRss(windowInfo, discordWebClient, getRobot());
                break;

            case RSS_FARMING:
            default:
                log.info("Executing RSS_FARMING for window: {}", windowInfo.getTitle());
                coreMechanics.findAndFarm(
                    marchConfig.getTargetRssLevel(),
                    getRssTypeFromConfig(),
                    windowInfo,
                    hasEncampments,
                    getRobot()
                );
                break;
        }
    }

    /**
     * Get RSS type from configuration with random selection support.
     */
    private RssType getRssTypeFromConfig() {
        return switch (marchConfig.getRssType()) {
            case "ALL" -> RssType.values()[random.nextInt(RssType.values().length)];
            case "ALL_WO_WS" -> RssType.values()[random.nextInt(RssType.values().length - 1)];
            default -> RssType.valueOf(marchConfig.getRssType());
        };
    }

    /**
     * Cleanup resources when worker terminates.
     */
    private void cleanup() {
        log.info("Cleaning up resources for window: {}", windowInfo.getTitle());

        // Robot cleanup - not strictly necessary as Robot has no explicit cleanup,
        // but setting to null allows GC
        if (robot != null) {
            robot = null;
            log.info("Released Robot instance for window: {}", windowInfo.getTitle());
        }

        log.info("Worker cleanup complete for window: {}", windowInfo.getTitle());
    }
}
