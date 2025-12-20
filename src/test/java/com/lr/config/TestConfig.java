package com.lr.config;

import com.lr.utils.WindowInputService;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;

/**
 * Test configuration that provides conditional beans for testing.
 * This allows tests to run with mock components in any environment.
 * Updated for JNA-based WindowInputService (no Robot dependency).
 */
@TestConfiguration
public class TestConfig {

    private final Environment environment;

    public TestConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Provides a mock WindowInputService for tests.
     * Returns a mock that simulates window operations without requiring real windows.
     */
    @Bean
    @Primary
    public WindowInputService windowInputService() {
        System.out.println("Creating MOCK WindowInputService for tests");
        WindowInputService mockService = mock(WindowInputService.class);

        // Configure the mock to return safe values for screen capture
        // Handle both null HWND and non-null HWND cases using any() which matches null too
        BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(mockService.captureWindow(any())).thenReturn(mockImage);

        // Configure screenToClient to return reasonable values
        // Using any() which matches null and non-null HWND values
        when(mockService.screenToClient(any(), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                int x = invocation.getArgument(1);
                int y = invocation.getArgument(2);
                return new int[]{x, y}; // Return same coords for simplicity
            });

        return mockService;
    }

    /**
     * Provides a test-specific ExecutorService for testing.
     * Uses a single-threaded executor for predictable test behavior.
     */
    @Bean
    @Primary
    public ExecutorService executorService() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * Provides a test-specific Random bean for predictable test behavior.
     */
    @Bean
    @Primary
    public java.util.Random random() {
        return new java.util.Random(42L); // Fixed seed for predictable tests
    }
}
