package com.lr.utils;

import com.sun.jna.platform.win32.WinDef.HWND;

import java.awt.image.BufferedImage;

/**
 * Service for sending input to windows without requiring focus.
 * Uses native Windows messages (SendMessage/PostMessage) to enable
 * true parallel automation of multiple windows.
 */
public interface WindowInputService {

    /**
     * Performs a left mouse click at the specified position.
     * Coordinates are client-relative (relative to window's top-left corner).
     *
     * @param hwnd Target window handle
     * @param clientX X coordinate (client-relative)
     * @param clientY Y coordinate (client-relative)
     */
    void leftClick(HWND hwnd, int clientX, int clientY);

    /**
     * Scrolls the mouse wheel at the specified position.
     *
     * @param hwnd Target window handle
     * @param clientX X coordinate (client-relative)
     * @param clientY Y coordinate (client-relative)
     * @param delta Wheel delta (positive = up/away, negative = down/toward)
     */
    void mouseWheel(HWND hwnd, int clientX, int clientY, int delta);

    /**
     * Sends a complete key tap (press + release).
     *
     * @param hwnd Target window handle
     * @param virtualKeyCode Virtual key code (e.g., VK_ESCAPE, VK_D)
     */
    void keyTap(HWND hwnd, int virtualKeyCode);

    /**
     * Sends a key press event (key down only).
     *
     * @param hwnd Target window handle
     * @param virtualKeyCode Virtual key code
     */
    void keyPress(HWND hwnd, int virtualKeyCode);

    /**
     * Sends a key release event (key up only).
     *
     * @param hwnd Target window handle
     * @param virtualKeyCode Virtual key code
     */
    void keyRelease(HWND hwnd, int virtualKeyCode);

    /**
     * Captures a screenshot of the specified window.
     * Works even if the window is in the background.
     *
     * @param hwnd Target window handle
     * @return BufferedImage containing the window screenshot
     */
    BufferedImage captureWindow(HWND hwnd);

    /**
     * Converts absolute screen coordinates to client-relative coordinates.
     *
     * @param hwnd Target window handle
     * @param screenX Absolute screen X coordinate
     * @param screenY Absolute screen Y coordinate
     * @return int[] with {clientX, clientY}
     */
    int[] screenToClient(HWND hwnd, int screenX, int screenY);

    /**
     * Adds a delay after input operations.
     * Useful for ensuring the target application has time to process input.
     *
     * @param millis Delay in milliseconds
     */
    void delay(long millis);
}
