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

@Configuration
@Slf4j
public class Beans {

    final
    GeneralConfig generalConfig;


    @Autowired
    public Beans(GeneralConfig generalConfig) {
        this.generalConfig = generalConfig;
    }

                    @Bean
                public Robot sharedRobot() throws AWTException {
                    // Log environment variables for debugging
                    String display = System.getenv("DISPLAY");
                    String javaAwtHeadless = System.getProperty("java.awt.headless");
                    boolean isHeadless = GraphicsEnvironment.isHeadless();
                    
                    log.info("Environment check - DISPLAY: {}, java.awt.headless: {}, GraphicsEnvironment.isHeadless(): {}", 
                            display, javaAwtHeadless, isHeadless);
                    
                    // Check the java.awt.headless property first (this is what we can control)
                    if ("true".equals(javaAwtHeadless)) {
                        log.info("Detected headless environment via java.awt.headless=true - creating stub Robot implementation");
                        return createStubRobot();
                    } else if (isHeadless) {
                        log.info("Detected headless environment via GraphicsEnvironment.isHeadless() - creating stub Robot implementation");
                        return createStubRobot();
                    } else {
                        log.info("Detected GUI environment - creating real Robot with display support");
                        try {
                            return new Robot();
                        } catch (AWTException e) {
                            log.error("Failed to create Robot in GUI environment: {}", e.getMessage());
                            throw e;
                        }
                    }
                }
    
    /**
     * Creates a stub Robot implementation for headless environments.
     * This satisfies dependency injection requirements without requiring a display.
     */
    private Robot createStubRobot() throws AWTException {
        log.info("Creating stub Robot for headless environment");
        // In headless environments, we need to set the headless property before creating Robot
        System.setProperty("java.awt.headless", "true");
        
        try {
            // Now try to create a real Robot with headless mode enabled
            return new Robot();
        } catch (AWTException e) {
            log.error("Failed to create Robot even with headless mode: {}. This is unexpected.", e.getMessage());
            throw new RuntimeException("Cannot create Robot bean in headless environment - this should not happen with proper headless configuration", e);
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
