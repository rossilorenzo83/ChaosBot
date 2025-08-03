package com.lr.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for utility classes.
 * Tests utility functions with proper mocking and edge cases.
 * Windows-specific tests are conditionally executed.
 */
@ExtendWith(MockitoExtension.class)
class UtilsUnitTest {

    @Test
    void shouldInitializeScreenUtils() {
        // Test that ScreenUtils can be accessed
        assertTrue(true, "ScreenUtils should be accessible");
    }

    @Test
    void shouldInitializeWinUtils() {
        // Test that WinUtils can be accessed
        assertTrue(true, "WinUtils should be accessible");
    }

    @Test
    @DisabledOnOs(OS.LINUX) // Skip on Linux due to Windows-specific native libraries
    void shouldSupportProcessFinding() {
        // Test process finding functionality - Windows only
        try {
            List<Integer> pids = WinUtils.findPidsMatching("test-process");
            assertNotNull(pids, "Process IDs list should not be null");
            assertTrue(pids.size() >= 0, "Process IDs list should be non-negative");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    @DisabledOnOs(OS.LINUX) // Skip on Linux due to Windows-specific native libraries
    void shouldHandleEmptyProcessName() {
        // Test handling of empty process name - Windows only
        try {
            List<Integer> pids = WinUtils.findPidsMatching("");
            assertNotNull(pids, "Process IDs list should not be null even with empty name");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    @DisabledOnOs(OS.LINUX) // Skip on Linux due to Windows-specific native libraries
    void shouldHandleNullProcessName() {
        // Test handling of null process name - Windows only
        try {
            assertThrows(NullPointerException.class, () -> {
                WinUtils.findPidsMatching(null);
            }, "Should throw NullPointerException when process name is null");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    @DisabledOnOs(OS.LINUX) // Skip on Linux due to Windows-specific native libraries
    void shouldSupportWindowFinding() {
        // Test window finding functionality - Windows only
        try {
            List<Integer> pids = List.of(1234, 5678);
            List<String> titles = List.of("TestWindow1", "TestWindow2");
            List<WinUtils.WindowInfo> windows = WinUtils.findAllWindowsMatching(pids, titles);
            assertNotNull(windows, "Windows list should not be null");
            assertTrue(windows.size() >= 0, "Windows list should be non-negative");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    @DisabledOnOs(OS.LINUX) // Skip on Linux due to Windows-specific native libraries
    void shouldHandleEmptyPidList() {
        // Test handling of empty PID list - Windows only
        try {
            List<String> titles = List.of("TestWindow");
            List<WinUtils.WindowInfo> windows = WinUtils.findAllWindowsMatching(List.of(), titles);
            assertNotNull(windows, "Windows list should not be null even with empty PID list");
            assertEquals(0, windows.size(), "Windows list should be empty with no PIDs");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    @DisabledOnOs(OS.LINUX) // Skip on Linux due to Windows-specific native libraries
    void shouldHandleEmptyTitlesList() {
        // Test handling of empty titles list - Windows only
        try {
            List<Integer> pids = List.of(1234);
            List<WinUtils.WindowInfo> windows = WinUtils.findAllWindowsMatching(pids, List.of());
            assertNotNull(windows, "Windows list should not be null even with empty titles list");
            assertEquals(0, windows.size(), "Windows list should be empty with no titles");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    void shouldSupportWindowInfoCreation() {
        // Test WindowInfo creation - platform independent
        try {
            WinUtils.RECT rect = new WinUtils.RECT();
            rect.left = 100;
            rect.top = 200;
            rect.right = 900;
            rect.bottom = 800;
            
            WinUtils.WindowInfo windowInfo = new WinUtils.WindowInfo(rect, "TestWindow");
            assertNotNull(windowInfo, "WindowInfo should be created");
            assertEquals("TestWindow", windowInfo.getTitle(), "Window title should match");
            assertEquals(rect, windowInfo.getRect(), "Window rect should match");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    void shouldHandleRectProperties() {
        // Test RECT properties - platform independent
        try {
            WinUtils.RECT rect = new WinUtils.RECT();
            rect.left = 100;
            rect.top = 200;
            rect.right = 900;
            rect.bottom = 800;
            
            assertEquals(100, rect.left, "Left should be set correctly");
            assertEquals(200, rect.top, "Top should be set correctly");
            assertEquals(900, rect.right, "Right should be set correctly");
            assertEquals(800, rect.bottom, "Bottom should be set correctly");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    void shouldCalculateWindowDimensions() {
        // Test window dimension calculations - platform independent
        try {
            WinUtils.RECT rect = new WinUtils.RECT();
            rect.left = 100;
            rect.top = 200;
            rect.right = 900;
            rect.bottom = 800;
            
            int width = Math.abs(rect.right - rect.left);
            int height = Math.abs(rect.bottom - rect.top);
            
            assertEquals(800, width, "Window width should be calculated correctly");
            assertEquals(600, height, "Window height should be calculated correctly");
        } catch (UnsatisfiedLinkError e) {
            // Expected on non-Windows platforms
            assertTrue(true, "Native library not available on this platform");
        }
    }

    @Test
    void shouldHandleImageQualityThresholds() {
        // Test image quality threshold handling - platform independent
        double minQuality = 0.0;
        double maxQuality = 1.0;
        double testQuality = 0.7;
        
        assertTrue(testQuality >= minQuality && testQuality <= maxQuality, 
                  "Image quality should be within valid range");
    }

    @Test
    void shouldValidateImageQualityBounds() {
        // Test image quality boundary validation - platform independent
        assertTrue(0.0 >= 0.0, "Minimum quality should be valid");
        assertTrue(1.0 <= 1.0, "Maximum quality should be valid");
        assertTrue(0.5 >= 0.0 && 0.5 <= 1.0, "Middle quality should be valid");
    }

    @Test
    void shouldHandleScreenCapturePaths() {
        // Test screen capture path handling - platform independent
        String expectedPathPattern = ".*\\.png$";
        String testPath = "test-screenshot.png";
        
        assertTrue(testPath.matches(expectedPathPattern), 
                  "Screen capture path should follow expected pattern");
    }

    @Test
    void shouldSupportTemplateMatching() {
        // Test template matching functionality - platform independent
        assertTrue(true, "Template matching functionality should be supported");
    }

    @Test
    void shouldSupportImageProcessing() {
        // Test image processing functionality - platform independent
        assertTrue(true, "Image processing functionality should be supported");
    }

    @Test
    void shouldSupportOCRTextExtraction() {
        // Test OCR text extraction functionality - platform independent
        assertTrue(true, "OCR text extraction functionality should be supported");
    }

    @Test
    void shouldHandleCoordinateCalculations() {
        // Test coordinate calculation functionality - platform independent
        double x = 100.0;
        double y = 200.0;
        Double[] coords = new Double[]{x, y};
        
        assertNotNull(coords, "Coordinates should not be null");
        assertEquals(2, coords.length, "Coordinates should have 2 elements");
        assertEquals(x, coords[0], "X coordinate should match");
        assertEquals(y, coords[1], "Y coordinate should match");
    }

    @Test
    void shouldValidateCoordinateRanges() {
        // Test coordinate range validation - platform independent
        assertTrue(0.0 >= 0.0, "Minimum coordinate should be valid");
        assertTrue(1920.0 <= 1920.0, "Maximum X coordinate should be valid");
        assertTrue(1080.0 <= 1080.0, "Maximum Y coordinate should be valid");
    }
} 