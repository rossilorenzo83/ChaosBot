package com.lr.config;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.LoadLibs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Arrays;
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

    @Bean
    public Robot sharedRobot() throws AWTException {
        // Log environment variables for debugging
        String display = System.getenv("DISPLAY");
        String javaAwtHeadless = System.getProperty("java.awt.headless");
        boolean isHeadless = GraphicsEnvironment.isHeadless();

        log.info("Environment check - DISPLAY: {}, java.awt.headless: {}, GraphicsEnvironment.isHeadless(): {}",
                display, javaAwtHeadless, isHeadless);

        // Try to create a real Robot first
        try {
            log.info("Attempting to create real Robot");
            return new Robot();
        } catch (AWTException e) {
            log.warn("Failed to create real Robot: {}. This is expected in headless environments.", e.getMessage());
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
