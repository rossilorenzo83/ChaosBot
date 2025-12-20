package com.lr.utils;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFOHEADER;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * JNA-based implementation of WindowInputService.
 * Uses Windows SendMessage/PostMessage APIs to send input to windows
 * without requiring focus, enabling true parallel automation.
 */
@Service
@Slf4j
public class JnaWindowInputService implements WindowInputService {

    // Windows Message Constants
    public static final int WM_MOUSEMOVE = 0x0200;
    public static final int WM_LBUTTONDOWN = 0x0201;
    public static final int WM_LBUTTONUP = 0x0202;
    public static final int WM_MOUSEWHEEL = 0x020A;
    public static final int WM_KEYDOWN = 0x0100;
    public static final int WM_KEYUP = 0x0101;

    // Mouse button state for wParam
    public static final int MK_LBUTTON = 0x0001;

    // Wheel delta (Windows standard)
    public static final int WHEEL_DELTA = 120;

    // PrintWindow flags
    public static final int PW_RENDERFULLCONTENT = 0x2;

    // Raster operation for BitBlt
    public static final int SRCCOPY = 0x00CC0020;

    // GetDeviceCaps constants for DPI detection
    public static final int HORZRES = 8;           // Logical width in pixels
    public static final int DESKTOPHORZRES = 118;  // Physical width in pixels

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "")
            .toLowerCase().contains("windows");

    /**
     * Extended User32 interface with SendMessage, PostMessage, and screen capture functions.
     */
    public interface User32Ex extends StdCallLibrary {
        User32Ex INSTANCE = IS_WINDOWS
                ? Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS)
                : null;

        LRESULT SendMessageA(HWND hWnd, int msg, WPARAM wParam, LPARAM lParam);

        boolean PostMessageA(HWND hWnd, int msg, WPARAM wParam, LPARAM lParam);

        boolean ScreenToClient(HWND hWnd, WinDef.POINT lpPoint);

        boolean ClientToScreen(HWND hWnd, WinDef.POINT lpPoint);

        boolean GetClientRect(HWND hWnd, RECT lpRect);

        boolean GetWindowRect(HWND hWnd, RECT lpRect);

        boolean PrintWindow(HWND hwnd, HDC hdcBlt, int nFlags);

        HDC GetDC(HWND hWnd);

        HDC GetWindowDC(HWND hWnd);

        int ReleaseDC(HWND hWnd, HDC hDC);

        int MapVirtualKeyA(int uCode, int uMapType);
    }

    /**
     * Extended GDI32 interface for bitmap operations.
     */
    public interface GDI32Ex extends StdCallLibrary {
        GDI32Ex INSTANCE = IS_WINDOWS
                ? Native.load("gdi32", GDI32Ex.class, W32APIOptions.DEFAULT_OPTIONS)
                : null;

        HDC CreateCompatibleDC(HDC hdc);

        HBITMAP CreateCompatibleBitmap(HDC hdc, int width, int height);

        HANDLE SelectObject(HDC hdc, HANDLE h);

        boolean DeleteObject(HANDLE hObject);

        boolean DeleteDC(HDC hdc);

        boolean BitBlt(HDC hdcDest, int xDest, int yDest, int w, int h,
                       HDC hdcSrc, int xSrc, int ySrc, int rop);

        int GetDIBits(HDC hdc, HBITMAP hbmp, int uStartScan, int cScanLines,
                      Pointer lpvBits, BITMAPINFO lpbi, int uUsage);

        int GetDeviceCaps(HDC hdc, int index);
    }

    /**
     * Shcore interface for modern DPI awareness.
     */
    public interface ShcoreLib extends StdCallLibrary {
        int PROCESS_PER_MONITOR_DPI_AWARE = 2;

        int SetProcessDpiAwareness(int awareness);
    }

    /**
     * Extended User32 for DPI functions.
     */
    public interface User32DPI extends StdCallLibrary {
        User32DPI INSTANCE = IS_WINDOWS
                ? Native.load("user32", User32DPI.class, W32APIOptions.DEFAULT_OPTIONS)
                : null;

        int GetDpiForWindow(HWND hwnd);
        boolean SetProcessDPIAware();
    }

    // Static initializer for DPI awareness
    static {
        if (IS_WINDOWS) {
            try {
                // Try modern per-monitor DPI awareness first (Windows 8.1+)
                ShcoreLib shcore = Native.load("shcore", ShcoreLib.class, W32APIOptions.DEFAULT_OPTIONS);
                int result = shcore.SetProcessDpiAwareness(ShcoreLib.PROCESS_PER_MONITOR_DPI_AWARE);
                if (result == 0) {
                    log.info("Per-monitor DPI awareness set successfully");
                } else if (result == 0x80070005) { // E_ACCESSDENIED - already set
                    log.info("DPI awareness already configured by JVM");
                } else {
                    log.warn("SetProcessDpiAwareness returned 0x{}, trying legacy API", Integer.toHexString(result));
                    User32DPI.INSTANCE.SetProcessDPIAware();
                    log.info("Legacy DPI awareness set");
                }
            } catch (UnsatisfiedLinkError e) {
                // shcore.dll not available (Windows 7 or earlier)
                try {
                    User32DPI.INSTANCE.SetProcessDPIAware();
                    log.info("Legacy DPI awareness set (shcore not available)");
                } catch (Exception e2) {
                    log.warn("Could not set DPI awareness: {}", e2.getMessage());
                }
            } catch (Exception e) {
                log.warn("DPI awareness setup failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public void leftClick(HWND hwnd, int clientX, int clientY) {
        if (!IS_WINDOWS || User32Ex.INSTANCE == null) {
            log.warn("leftClick called on non-Windows platform");
            return;
        }

        LPARAM lParam = makeLParam(clientX, clientY);

        // Move mouse to position
        User32Ex.INSTANCE.PostMessageA(hwnd, WM_MOUSEMOVE, new WPARAM(0), lParam);

        // Small delay for realistic input
        delay(5);

        // Press left button
        User32Ex.INSTANCE.PostMessageA(hwnd, WM_LBUTTONDOWN, new WPARAM(MK_LBUTTON), lParam);

        delay(10);

        // Release left button
        User32Ex.INSTANCE.PostMessageA(hwnd, WM_LBUTTONUP, new WPARAM(0), lParam);

        log.debug("leftClick at ({}, {}) on hwnd {}", clientX, clientY, hwnd);
    }

    @Override
    public void mouseWheel(HWND hwnd, int clientX, int clientY, int delta) {
        if (!IS_WINDOWS || User32Ex.INSTANCE == null) {
            log.warn("mouseWheel called on non-Windows platform");
            return;
        }

        LPARAM lParam = makeLParam(clientX, clientY);

        // wParam: high word = wheel delta in WHEEL_DELTA units
        int wParamValue = (delta * WHEEL_DELTA) << 16;

        User32Ex.INSTANCE.PostMessageA(hwnd, WM_MOUSEWHEEL, new WPARAM(wParamValue), lParam);

        log.debug("mouseWheel delta {} at ({}, {}) on hwnd {}", delta, clientX, clientY, hwnd);
    }

    @Override
    public void keyTap(HWND hwnd, int virtualKeyCode) {
        keyPress(hwnd, virtualKeyCode);
        delay(50);
        keyRelease(hwnd, virtualKeyCode);
    }

    @Override
    public void keyPress(HWND hwnd, int virtualKeyCode) {
        if (!IS_WINDOWS || User32Ex.INSTANCE == null) {
            log.warn("keyPress called on non-Windows platform");
            return;
        }

        // Map virtual key to scan code
        int scanCode = User32Ex.INSTANCE.MapVirtualKeyA(virtualKeyCode, 0);

        // lParam: bits 0-15 = repeat count (1), bits 16-23 = scan code
        LPARAM lParam = new LPARAM((scanCode << 16) | 1);

        User32Ex.INSTANCE.PostMessageA(hwnd, WM_KEYDOWN, new WPARAM(virtualKeyCode), lParam);

        log.debug("keyPress VK={} on hwnd {}", virtualKeyCode, hwnd);
    }

    @Override
    public void keyRelease(HWND hwnd, int virtualKeyCode) {
        if (!IS_WINDOWS || User32Ex.INSTANCE == null) {
            log.warn("keyRelease called on non-Windows platform");
            return;
        }

        int scanCode = User32Ex.INSTANCE.MapVirtualKeyA(virtualKeyCode, 0);

        // lParam for key up: bit 30 = previous key state (1), bit 31 = transition state (1)
        long lParamValue = (scanCode << 16) | 1 | (1L << 30) | (1L << 31);
        LPARAM lParam = new LPARAM(lParamValue);

        User32Ex.INSTANCE.PostMessageA(hwnd, WM_KEYUP, new WPARAM(virtualKeyCode), lParam);

        log.debug("keyRelease VK={} on hwnd {}", virtualKeyCode, hwnd);
    }

    @Override
    public BufferedImage captureWindow(HWND hwnd) {
        if (!IS_WINDOWS || User32Ex.INSTANCE == null || GDI32Ex.INSTANCE == null) {
            log.warn("captureWindow called on non-Windows platform");
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }

        try {
            // Get window dimensions
            RECT rect = new RECT();
            User32Ex.INSTANCE.GetWindowRect(hwnd, rect);
            int width = rect.right - rect.left;
            int height = rect.bottom - rect.top;

            if (width <= 0 || height <= 0) {
                log.error("Invalid window dimensions: {}x{}", width, height);
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            }

            log.info("Capturing window: {}x{} at ({},{})", width, height, rect.left, rect.top);

            // Use PrintWindow to capture the window contents directly
            // This works even when the window is partially obscured or in background
            HDC hdcWindow = User32Ex.INSTANCE.GetWindowDC(hwnd);
            HDC hdcMemDC = GDI32Ex.INSTANCE.CreateCompatibleDC(hdcWindow);
            HBITMAP hBitmap = GDI32Ex.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);
            HANDLE oldBitmap = GDI32Ex.INSTANCE.SelectObject(hdcMemDC, hBitmap);

            // PrintWindow captures the window content regardless of visibility
            // PW_RENDERFULLCONTENT (0x2) ensures we get the full content including DirectX/OpenGL
            boolean success = User32Ex.INSTANCE.PrintWindow(hwnd, hdcMemDC, PW_RENDERFULLCONTENT);

            if (!success) {
                log.warn("PrintWindow failed, falling back to BitBlt from window DC");
                // Fallback: try BitBlt from window DC (still captures window content, not screen)
                success = GDI32Ex.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height,
                        hdcWindow, 0, 0, SRCCOPY);
                if (!success) {
                    log.error("BitBlt fallback also failed");
                }
            }

            // Convert HBITMAP to BufferedImage
            BufferedImage image = hbitmapToBufferedImage(hdcMemDC, hBitmap, width, height);

            // Cleanup GDI objects
            GDI32Ex.INSTANCE.SelectObject(hdcMemDC, oldBitmap);
            GDI32Ex.INSTANCE.DeleteObject(hBitmap);
            GDI32Ex.INSTANCE.DeleteDC(hdcMemDC);
            User32Ex.INSTANCE.ReleaseDC(hwnd, hdcWindow);

            return image;

        } catch (Exception e) {
            log.error("Failed to capture window", e);
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }
    }

    @Override
    public int[] screenToClient(HWND hwnd, int screenX, int screenY) {
        if (!IS_WINDOWS || User32Ex.INSTANCE == null) {
            return new int[]{screenX, screenY};
        }

        WinDef.POINT point = new WinDef.POINT(screenX, screenY);
        User32Ex.INSTANCE.ScreenToClient(hwnd, point);
        return new int[]{point.x, point.y};
    }

    @Override
    public void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates LPARAM from x,y coordinates.
     * Low word = x, high word = y.
     */
    private LPARAM makeLParam(int x, int y) {
        return new LPARAM((y << 16) | (x & 0xFFFF));
    }

    /**
     * Converts an HBITMAP to a BufferedImage.
     */
    private BufferedImage hbitmapToBufferedImage(HDC hdcMemDC, HBITMAP hBitmap, int width, int height) {
        // Setup BITMAPINFO for GetDIBits
        BITMAPINFO bmi = new BITMAPINFO();
        bmi.bmiHeader = new BITMAPINFOHEADER();
        bmi.bmiHeader.biSize = bmi.bmiHeader.size();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height; // Negative for top-down DIB
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        // Allocate memory for pixel data
        Memory buffer = new Memory((long) width * height * 4);

        // Get the bits
        int scanLines = GDI32Ex.INSTANCE.GetDIBits(hdcMemDC, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);
        log.debug("GetDIBits returned {} scan lines (expected {})", scanLines, height);

        // Create BufferedImage - use TYPE_3BYTE_BGR for compatibility
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] imgData = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();

        // Copy pixels - Windows DIB is BGRA, TYPE_3BYTE_BGR stores as B,G,R
        int nonZeroPixels = 0;
        for (int i = 0; i < width * height; i++) {
            int bgra = buffer.getInt((long) i * 4);
            byte b = (byte) ((bgra >> 0) & 0xFF);
            byte g = (byte) ((bgra >> 8) & 0xFF);
            byte r = (byte) ((bgra >> 16) & 0xFF);

            imgData[i * 3] = b;
            imgData[i * 3 + 1] = g;
            imgData[i * 3 + 2] = r;

            if (b != 0 || g != 0 || r != 0) nonZeroPixels++;
        }
        log.debug("Image has {} non-zero pixels out of {}", nonZeroPixels, width * height);

        return image;
    }
}
