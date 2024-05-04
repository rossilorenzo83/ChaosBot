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
        return new Robot();
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
