package com.lr.business;

import com.lr.config.GeneralConfig;
import com.lr.config.MarchConfig;
import com.lr.utils.WindowInputService;
import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.web.reactive.function.client.WebClient;

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
 * Uses focus-independent JNA PostMessage/SendMessage for parallel execution.
 *
 * This class encapsulates:
 * - Window-specific state (availMarches, firstRun, etc.)
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
    private final WindowInputService windowInputService;

    // Per-worker state
    private Integer availMarches;
    private Boolean firstRun = true;
    private Long timeLastActionPerformed;
    private volatile boolean shutdownRequested = false;

    /**
     * Creates a new window automation worker.
     * Uses WindowInputService for focus-independent input.
     */
    public WindowAutomationWorker(
            WinUtils.WindowInfo windowInfo,
            CoreMechanics coreMechanics,
            GeneralConfig generalConfig,
            MarchConfig marchConfig,
            WebClient discordWebClient,
            Random random,
            WindowInputService windowInputService) {

        this.windowInfo = windowInfo;
        this.coreMechanics = coreMechanics;
        this.generalConfig = generalConfig;
        this.marchConfig = marchConfig;
        this.discordWebClient = discordWebClient;
        this.random = random;
        this.windowInputService = windowInputService;
        this.availMarches = marchConfig.getMarchesAvailable();
        this.timeLastActionPerformed = System.currentTimeMillis();
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
        } catch (IOException | URISyntaxException e) {
            log.error("Fatal error in automation worker for window: {}", windowInfo.getTitle(), e);

        } finally {
            cleanup();
        }
    }

    /**
     * Initialize window coordinates and setup.
     */
    private void initializeWindow() throws IOException, URISyntaxException {
        log.info("Initializing window: {}", windowInfo.getTitle());

        String fullImagePath = takeScreenCapture(windowInfo, windowInputService);
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
     *
     * IMPORTANT: Action failures are caught and logged but don't terminate the loop.
     * The worker will continue and retry on the next interval.
     */
    private void processWindowLoop() throws InterruptedException {
        log.info("Entering main processing loop for window: {}", windowInfo.getTitle());

        while (!shutdownRequested && !Thread.currentThread().isInterrupted()) {
            // Check if timer expired and reset marches
            if (availMarches == 0) {
                long elapsedMs = System.currentTimeMillis() - timeLastActionPerformed;
                long intervalMs = marchConfig.getMarchesIntervalMins() * 60L * 1000L;

                log.debug("Timer check for window: {} - elapsed: {}ms, interval: {}ms, remaining: {}ms",
                    windowInfo.getTitle(), elapsedMs, intervalMs, intervalMs - elapsedMs);

                if (elapsedMs > intervalMs) {
                    log.info("Timer expired for window: {}. Resetting marches to {}.",
                        windowInfo.getTitle(), marchConfig.getMarchesAvailable());
                    availMarches = marchConfig.getMarchesAvailable();
                    firstRun = true;
                }
            }

            // Process actions if marches available
            if (availMarches > 0) {
                log.info("Processing action for window: {} (marches remaining: {})",
                    windowInfo.getTitle(), availMarches);

                try {
                    File tmpFolder = LoadLibs.extractTessResources("win32-x86-64");
                    log.debug("Tesseract tmp folder path: {}", tmpFolder.getPath());
                    System.setProperty("java.library.path", tmpFolder.getPath());

                    performAction();

                    // Only decrement marches and update time on SUCCESS
                    availMarches--;
                    firstRun = false;
                    timeLastActionPerformed = System.currentTimeMillis();

                    log.info("Action completed successfully for window: {}. Marches remaining: {}",
                        windowInfo.getTitle(), availMarches);

                } catch (Exception e) {
                    // Catch ALL exceptions - loop must NEVER die from errors like:
                    // ImageNotMatchedException, template not found, coords not found, etc.
                    log.warn("Action failed for window: {}. Will retry on next interval. Error: {} - {}",
                        windowInfo.getTitle(), e.getClass().getSimpleName(), e.getMessage());

                    // Still decrement marches to prevent infinite retry loop
                    availMarches--;
                    timeLastActionPerformed = System.currentTimeMillis();

                    // Small delay before continuing to avoid rapid failure loops
                    windowInputService.delay(generalConfig.getActionIntervalMs());
                }
            }

            // Small sleep to prevent tight loop when no marches available
            if (availMarches == 0) {
                // Log status every 5 minutes to confirm loop is running
                long elapsedMs = System.currentTimeMillis() - timeLastActionPerformed;
                long intervalMs = marchConfig.getMarchesIntervalMins() * 60L * 1000L;
                long remainingSec = (intervalMs - elapsedMs) / 1000;

                if (remainingSec % 300 == 0 && remainingSec >= 0) {
                    log.info("Waiting for timer reset on window: {}. {} minutes remaining until next action cycle.",
                        windowInfo.getTitle(), remainingSec / 60);
                }

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
     *
     * All CoreMechanics methods now use focus-independent PostMessage/SendMessage,
     * allowing true parallel execution without any global synchronization.
     */
    private void performAction() throws IOException, URISyntaxException, TesseractException {
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
                    firstRun
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
                    coreMechanics.challengeStats(windowInfo, discordWebClient, challengeViewButton);
                }
                break;

            case DONORS_STATS:
                log.info("Executing DONORS_STATS for window: {}", windowInfo.getTitle());
                coreMechanics.receivedRss(windowInfo, discordWebClient);
                break;

            case RSS_FARMING:
            default:
                log.info("Executing RSS_FARMING for window: {}", windowInfo.getTitle());
                coreMechanics.findAndFarm(
                    marchConfig.getTargetRssLevel(),
                    getRssTypeFromConfig(),
                    windowInfo,
                    hasEncampments
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
        // No explicit cleanup needed - WindowInputService is stateless
        log.info("Worker cleanup complete for window: {}", windowInfo.getTitle());
    }
}
