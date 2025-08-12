package com.lr.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

/**
 * Test configuration for Tess4J 5.16.0 compatibility.
 * Provides mock Tesseract instances for testing without requiring actual OCR data.
 * Updated for Tess4J 5.16.0 and Spring Boot 3.5.4 compatibility.
 */
@TestConfiguration
@ActiveProfiles("test")
public class Tess4JTestConfig {

    /**
     * Provides a test-specific Tesseract instance.
     * Uses mock configuration to avoid requiring actual OCR training data.
     */
    @Bean
    @Primary
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        
        try {
            // Set test-specific configuration
            tesseract.setDatapath(getTestDataPath());
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(1);
            tesseract.setOcrEngineMode(1);
            
            // Set test-specific variables
            tesseract.setVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
            tesseract.setVariable("tessedit_pageseg_mode", "1");
            
        } catch (Exception e) {
            // If we can't configure Tesseract properly, create a minimal instance
            System.out.println("Warning: Could not configure Tesseract for testing: " + e.getMessage());
        }
        
        return tesseract;
    }

    /**
     * Get the test data path for Tess4J.
     * Returns a safe test path that won't interfere with production data.
     */
    private String getTestDataPath() {
        try {
            // Try to get the test data directory
            File testDataDir = new File("src/test/resources/test-data");
            if (testDataDir.exists() && testDataDir.isDirectory()) {
                return testDataDir.getAbsolutePath();
            }
            
            // Fallback to system temp directory
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "tess4j-test");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            return tempDir.getAbsolutePath();
            
        } catch (Exception e) {
            // Final fallback to current directory
            return ".";
        }
    }
}
