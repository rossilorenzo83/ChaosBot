package com.lr;

import com.lr.business.CoreMechanics;
import com.lr.business.WindowAutomationWorker;
import com.lr.config.GeneralConfig;
import com.lr.config.MarchConfig;
import com.lr.utils.WinUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableConfigurationProperties({GeneralConfig.class, MarchConfig.class})
@Slf4j
public class ChaosBot implements CommandLineRunner {

    private final CoreMechanics coreMechanics;
    private final ExecutorService executorService;
    private final Random random;
    private final GeneralConfig generalConfig;
    private final MarchConfig marchConfig;
    private final WebClient discordWebClient;
    private final com.lr.config.Beans.RobotFactory robotFactory;

    // Track workers for graceful shutdown
    private final List<WindowAutomationWorker> workers = new ArrayList<>();

    @Autowired
    public ChaosBot(CoreMechanics coreMechanics, ExecutorService executorService, Random random,
                   GeneralConfig generalConfig, MarchConfig marchConfig, WebClient discordWebClient,
                   com.lr.config.Beans.RobotFactory robotFactory) {
        this.coreMechanics = coreMechanics;
        this.executorService = executorService;
        this.random = random;
        this.generalConfig = generalConfig;
        this.marchConfig = marchConfig;
        this.discordWebClient = discordWebClient;
        this.robotFactory = robotFactory;
    }

    /**
     * Graceful shutdown handler - called by Spring on application shutdown (Ctrl+C, SIGTERM).
     */
    @PreDestroy
    public void onShutdown() {
        log.info("Shutdown signal received. Initiating graceful shutdown...");

        // Signal all workers to stop
        workers.forEach(WindowAutomationWorker::requestShutdown);
        log.info("Shutdown requested for all {} workers", workers.size());

        // Shutdown executor
        executorService.shutdown();
        log.info("Executor shutdown initiated. Waiting for workers to complete...");

        try {
            // Wait up to 30 seconds for workers to finish gracefully
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Workers did not terminate in time. Forcing shutdown...");
                executorService.shutdownNow();

                // Wait a bit more for forced shutdown
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Workers did not respond to forced shutdown!");
                }
            } else {
                log.info("All workers terminated gracefully.");
            }
        } catch (InterruptedException e) {
            log.error("Interrupted during shutdown", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    public static void main(String[] args) {
        SpringApplication.run(ChaosBot.class, args);
    }

    @Override
    public void run(String... args) {

        //Load dll from jar dep
        nu.pattern.OpenCV.loadLocally();
        List<Integer> pidsBS = WinUtils.findPidsMatching(generalConfig.getPidName());
        log.info("PIDs matching config found: {}", pidsBS.size());
        List<WinUtils.WindowInfo> hwndList = WinUtils.findAllWindowsMatching(pidsBS, generalConfig.getWindowsNames());
        log.info("Windows matching config found: {}", hwndList.size());

        if (hwndList.isEmpty()) {
            log.warn("No windows found matching configuration. Exiting.");
            return;
        }

        // Submit all window tasks to the executor using WindowAutomationWorker
        hwndList.forEach(windowInfo -> {
            // Create a worker for each window
            WindowAutomationWorker worker = new WindowAutomationWorker(
                windowInfo,
                coreMechanics,
                generalConfig,
                marchConfig,
                discordWebClient,
                random,
                robotFactory
            );

            // Track worker for shutdown (accessed by @PreDestroy)
            workers.add(worker);

            // Submit worker to executor
            executorService.execute(worker);
            log.info("Submitted worker for window: {}", windowInfo.getTitle());
        });

        log.info("Submitted {} window tasks to executor. Bot is now running...", hwndList.size());
        log.info("Press Ctrl+C to stop the bot gracefully.");

        // Keep main thread alive - wait for executor to terminate
        // (This will block until @PreDestroy shuts down the executor on Ctrl+C)
        try {
            // Wait indefinitely - shutdown is handled by @PreDestroy
            while (!executorService.isTerminated()) {
                executorService.awaitTermination(1, TimeUnit.HOURS);
            }
            log.info("All workers completed. Exiting.");
        } catch (InterruptedException e) {
            log.info("Main thread interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }

}


