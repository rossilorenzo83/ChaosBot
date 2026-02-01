package com.lr;

import com.lr.business.*;
import com.lr.config.GeneralConfig;
import com.lr.config.MarchConfig;
import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.reactive.function.client.WebClient;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static com.lr.business.CoreMechanics.CONVERT_IMG_FLAG;
import static com.lr.utils.ScreenUtils.*;

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


        try {
            // Submit all window tasks to the executor using WindowAutomationWorker
            hwndList.forEach(windowInfo -> {
                // Create a worker for each window
                com.lr.business.WindowAutomationWorker worker = new com.lr.business.WindowAutomationWorker(
                    windowInfo,
                    coreMechanics,
                    generalConfig,
                    marchConfig,
                    discordWebClient,
                    random,
                    robotFactory
                );

                // Submit worker to executor
                executorService.execute(worker);
                log.info("Submitted worker for window: {}", windowInfo.getTitle());
            });

            log.info("Submitted {} window tasks to executor", hwndList.size());

            // Shutdown executor and wait for tasks to complete
            executorService.shutdown();
            log.info("Executor shutdown initiated. Waiting for tasks to complete...");

            // Wait for all tasks to complete (or timeout after 1 hour per window)
            if (!executorService.awaitTermination(hwndList.size() * 60L, java.util.concurrent.TimeUnit.MINUTES)) {
                log.warn("Executor did not terminate in the specified time. Forcing shutdown...");
                executorService.shutdownNow();
            } else {
                log.info("All window tasks completed successfully.");
            }

        } catch (InterruptedException e) {
            log.error("Main thread interrupted during executor shutdown", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}


