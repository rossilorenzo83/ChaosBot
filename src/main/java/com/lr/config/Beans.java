package com.lr.config;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.LoadLibs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.core.env.Environment;

@Configuration
@Slf4j
public class Beans {

    final
    GeneralConfig generalConfig;

    @Autowired
    private Environment environment;


    @Autowired
    public Beans(GeneralConfig generalConfig) {
        this.generalConfig = generalConfig;
    }

    /**
     * Robot factory method for creating per-thread Robot instances.
     * Each thread should create its own Robot to avoid concurrency issues.
     *
     * NOTE: This is NOT a @Bean - Robot instances must be created per-thread, not as singleton.
     * Use RobotFactory bean to create Robot instances.
     */
    @Bean
    public RobotFactory robotFactory() {
        return new RobotFactory();
    }

    /**
     * Factory class for creating Robot instances with proper headless detection
     */
    public static class RobotFactory {

        public Robot createRobot() throws AWTException {
            // Log environment variables for debugging
            String display = System.getenv("DISPLAY");
            String javaAwtHeadless = System.getProperty("java.awt.headless");
            boolean isHeadless = GraphicsEnvironment.isHeadless();
            String osName = System.getProperty("os.name", "").toLowerCase();

            log.info("Creating Robot for thread: {} - DISPLAY: {}, java.awt.headless: {}, GraphicsEnvironment.isHeadless(): {}, OS: {}",
                    Thread.currentThread().getName(), display, javaAwtHeadless, isHeadless, osName);

            // Better headless detection logic
            boolean canCreateRealRobot = false;

            if (osName.contains("windows")) {
                // Windows: check if we have a GUI (not headless)
                canCreateRealRobot = !isHeadless;
            } else {
                // Linux/Unix: check if DISPLAY is available and not headless
                canCreateRealRobot = display != null && !display.isEmpty() && !isHeadless;
            }

            log.info("Can create real Robot: {}", canCreateRealRobot);

            // Try to create a real Robot only if we think we can
            if (canCreateRealRobot) {
                try {
                    log.info("Created Robot for thread: {}", Thread.currentThread().getName());
                    return new Robot();
                } catch (AWTException e) {
                    log.warn("Failed to create real Robot: {}. This is expected in headless environments.", e.getMessage());
                    // Fall through to throw exception
                }
            } else {
                log.info("Skipping real Robot creation - headless environment detected");
            }

            // In headless environments, we cannot create a Robot
            // We'll throw a more descriptive exception
            throw new AWTException("Cannot create Robot in headless environment. This application requires a display for automation features.");
        }
    }
    


    @Bean
    public Random sharedRandom() {
        return new Random();
    }

    @Bean
    public Tesseract ocrEngine() throws IOException {
        Tesseract tesseract = new Tesseract();
        log.info("Setting OCR lnguage to: {}", generalConfig.getGameLanguage().toString());
        tesseract.setLanguage(generalConfig.getGameLanguage().getISO3Language());
        tesseract.setOcrEngineMode(3);
        tesseract.setPageSegMode(3);

        File tmpDataFolder = LoadLibs.extractTessResources("tessdata");

        //Add french vocabulary
        File f = new File(tmpDataFolder, "fra.traineddata");
        java.nio.file.Files.copy(
                new ClassPathResource("data/fra.traineddata").getInputStream(),
                f.toPath(),
                StandardCopyOption.REPLACE_EXISTING);


        tesseract.setDatapath(tmpDataFolder.getPath());
        return tesseract;
    }

    @Bean
    public ExecutorService getThreadPool() {
        return Executors.newFixedThreadPool(generalConfig.getWindowsNames().size());
    }

}
