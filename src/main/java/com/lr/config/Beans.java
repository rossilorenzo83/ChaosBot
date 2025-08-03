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
                    
                    // Use DISPLAY variable as primary criterion for headless detection
                    // This is the most reliable indicator across different environments
                    if (display == null || display.isEmpty()) {
                        log.info("Detected headless environment via DISPLAY=null/empty - creating stub Robot implementation");
                        return createStubRobot();
                    } else if ("true".equals(javaAwtHeadless)) {
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
        
        // Create a stub Robot that doesn't require a display
        // This is a minimal implementation that satisfies the Robot interface
        return new Robot() {
            @Override
            public void mouseMove(int x, int y) {
                log.debug("Stub Robot: mouseMove({}, {})", x, y);
                // No-op in headless environment
            }
            
            @Override
            public void mousePress(int buttons) {
                log.debug("Stub Robot: mousePress({})", buttons);
                // No-op in headless environment
            }
            
            @Override
            public void mouseRelease(int buttons) {
                log.debug("Stub Robot: mouseRelease({})", buttons);
                // No-op in headless environment
            }
            
            @Override
            public void keyPress(int keycode) {
                log.debug("Stub Robot: keyPress({})", keycode);
                // No-op in headless environment
            }
            
            @Override
            public void keyRelease(int keycode) {
                log.debug("Stub Robot: keyRelease({})", keycode);
                // No-op in headless environment
            }
            
            @Override
            public BufferedImage createScreenCapture(Rectangle screenRect) {
                log.debug("Stub Robot: createScreenCapture({})", screenRect);
                // Return a blank image in headless environment
                return new BufferedImage(screenRect.width, screenRect.height, BufferedImage.TYPE_INT_RGB);
            }
            
            @Override
            public Color getPixelColor(int x, int y) {
                log.debug("Stub Robot: getPixelColor({}, {})", x, y);
                // Return black color in headless environment
                return Color.BLACK;
            }
            
            @Override
            public void setAutoDelay(int ms) {
                log.debug("Stub Robot: setAutoDelay({})", ms);
                // No-op in headless environment
            }
            
            @Override
            public void setAutoWaitForIdle(boolean isOn) {
                log.debug("Stub Robot: setAutoWaitForIdle({})", isOn);
                // No-op in headless environment
            }
            
            @Override
            public int getAutoDelay() {
                return 0; // Default delay
            }
            
            @Override
            public boolean isAutoWaitForIdle() {
                return false; // Default value
            }
        };
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
