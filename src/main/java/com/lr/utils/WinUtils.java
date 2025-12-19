package com.lr.utils;


import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.WinDef.DWORD;

import java.util.ArrayList;
import java.util.List;

public class WinUtils {

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("windows");

    public static List<Integer> findPidsMatching(String name) {
        if (!IS_WINDOWS) {
            // Return empty list on non-Windows platforms
            return new ArrayList<>();
        }
        
        try {
            int[] processlist = new int[5012];
            int[] dummylist = new int[5012];
            List<Integer> pidsBS = new ArrayList<>(5);
            WinUtils.Psapi.INSTANCE.EnumProcesses(processlist, 5012, dummylist);

            boolean enableDebugPrivilege = WinUtils.enableDebugPrivilege();

            for (int i = 0; i < processlist.length; i++) {
                WinNT.HANDLE ph = com.sun.jna.platform.win32.Kernel32.INSTANCE.OpenProcess(0x0400 | 0x0010, false, processlist[i]);
                if (ph != null) {

                    byte[] buffer = new byte[1024];
                    com.sun.jna.platform.win32.Psapi.INSTANCE.GetModuleFileNameExA(ph, new WinNT.HANDLE(), buffer, buffer.length);
                    String processName = Native.toString(buffer);


                    if (processName.contains(name)) {
                        pidsBS.add(processlist[i]);
                    }
                    com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(ph);
                }
            }
            return pidsBS;
        } catch (Exception e) {
            // Return empty list if any Windows-specific operation fails
            return new ArrayList<>();
        }
    }

    /**
     * Enables debug privileges for this process, required for OpenProcess() to get
     * processes other than the current user
     *
     * @return {@code true} if debug privileges were successfully enabled.
     */
    private static boolean enableDebugPrivilege() {
        if (!IS_WINDOWS) {
            return false;
        }
        
        try {
            HANDLEByReference hToken = new HANDLEByReference();
            boolean success = Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),
                    0x0008 | 0x0020, hToken);
            if (!success) {
                return false;
            }
            try {
                WinNT.LUID luid = new WinNT.LUID();
                success = Advapi32.INSTANCE.LookupPrivilegeValue(null, "SeDebugPrivilege", luid);
                if (!success) {
                    return false;
                }
                WinNT.TOKEN_PRIVILEGES tkp = new WinNT.TOKEN_PRIVILEGES(1);
                tkp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new DWORD(0x00000002));
                success = Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tkp, 0, null, null);
                int err = Native.getLastError();
                if (!success) {
                    return false;
                } else if (err == 1300) { // ERROR_NOT_ALL_ASSIGNED
                    return false;
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(hToken.getValue());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<WindowInfo> findAllWindowsMatching(List<Integer> pids, List<String> titlesFilter) {
        if (!IS_WINDOWS) {
            // Return empty list on non-Windows platforms
            return new ArrayList<>();
        }
        
        try {
            final List<WindowInfo> windows = new ArrayList<>();
            User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
                @Override
                public boolean callback(WinDef.HWND hWnd, Pointer arg) {
                    char[] buffer = new char[1024];
                    IntByReference pidPointer = new IntByReference();
                    User32.INSTANCE.GetWindowText(hWnd, buffer, buffer.length);
                    String title = Native.toString(buffer);
                    User32.INSTANCE.GetWindowThreadProcessId(hWnd, pidPointer);

                    int winPid = pidPointer.getValue();
                    if (pids.contains(winPid) && titlesFilter.contains(title)) {
                        windows.add(getWindowInfo(hWnd));
                    }
                    return true;
                }
            }, null);
            return windows;
        } catch (Exception e) {
            // Return empty list if any Windows-specific operation fails
            return new ArrayList<>();
        }
    }

    public static WindowInfo getWindowInfo(WinDef.HWND hWnd) {
        if (!IS_WINDOWS) {
            // Return a dummy WindowInfo on non-Windows platforms
            return new WindowInfo(new RECT(), "dummy");
        }
        
        try {
            RECT r = new RECT();
            User32.INSTANCE.GetWindowRect(hWnd, r);
            char[] buffer = new char[1024];
            User32.INSTANCE.GetWindowText(hWnd, buffer, buffer.length);
            String title = Native.toString(buffer);
            WindowInfo info = new WindowInfo(r, title);
            return info;
        } catch (Exception e) {
            // Return a dummy WindowInfo if any Windows-specific operation fails
            return new WindowInfo(new RECT(), "dummy");
        }
    }

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = IS_WINDOWS ? (User32) Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS) : null;

        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);

        int GetWindowText(WinDef.HWND hWnd, char[] lpString, int nMaxCount);

        int GetWindowRect(WinDef.HWND hWnd, RECT r);

        int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference pref);

        boolean SetForegroundWindow(WinDef.HWND hWnd);

        WinDef.HWND FindWindowA(String lpClassName, String lpWindowName);
    }

    /**
     * Brings a window to the foreground by its title.
     * @param windowTitle The title of the window to focus
     * @return true if the window was found and brought to foreground
     */
    public static boolean focusWindow(String windowTitle) {
        if (!IS_WINDOWS || User32.INSTANCE == null) {
            return false;
        }

        try {
            WinDef.HWND hwnd = User32.INSTANCE.FindWindowA(null, windowTitle);
            if (hwnd != null) {
                return User32.INSTANCE.SetForegroundWindow(hwnd);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public interface Psapi extends StdCallLibrary {
        Psapi INSTANCE = IS_WINDOWS ? (Psapi) Native.load("Psapi", Psapi.class) : null;

        boolean EnumProcesses(int[] ProcessIDsOut, int size, int[] BytesReturned);

        boolean EnumProcessModules(WinNT.HANDLE hProcess, WinDef.HMODULE[] lphModule, int cb, IntByReference lpcbNeeded);

        WinDef.DWORD GetModuleBaseNameW(Pointer hProcess, Pointer hModule, byte[] lpBaseName, int nSize);

        boolean GetModuleInformation(WinNT.HANDLE hProcess, WinDef.HMODULE hModule, com.sun.jna.platform.win32.Psapi.MODULEINFO lpmodinfo, int cb);

        int GetModuleFileNameExA(WinNT.HANDLE process, WinNT.HANDLE module, byte[] lpFilename, int nSize);
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = IS_WINDOWS ? (Kernel32) Native.load("Kernel32", Kernel32.class) : null;

        WinNT.HANDLE OpenProcess(int fdwAccess, boolean fInherit, int IDProcess);

        WinNT.HANDLE GetCurrentProcess();

        void CloseHandle(WinNT.HANDLE handle);
    }

    public static class RECT extends Structure {
        public int left, top, right, bottom;

        @Override
        protected List<String> getFieldOrder() {
            List<String> order = new ArrayList<>();
            order.add("left");
            order.add("top");
            order.add("right");
            order.add("bottom");
            return order;
        }
    }

    public static class WindowInfo {
        RECT rect;
        String title;

        public WindowInfo(RECT rect, String title) {
            this.rect = rect;
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public RECT getRect() {
            return rect;
        }

        public String toString() {
            return String.format("(%d,%d)-(%d,%d) : \"%s\"", rect.left, rect.top, rect.right, rect.bottom, title);
        }
    }
}
