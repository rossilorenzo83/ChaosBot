package com.lr.utils;


import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.ArrayList;
import java.util.List;

import static com.sun.jna.platform.win32.WinNT.*;

public class WinUtils {

    public static List<Integer> findPidsMatching(String name) {
        int[] processlist = new int[5012];
        int[] dummylist = new int[5012];
        List<Integer> pidsBS = new ArrayList<>(5);
        WinUtils.Psapi.INSTANCE.EnumProcesses(processlist, 5012, dummylist);

        boolean enableDebugPrivilege = WinUtils.enableDebugPrivilege();

        //FIXME improve to better filter out
        for (int i = 0; i < processlist.length; i++) {
            WinNT.HANDLE ph = com.sun.jna.platform.win32.Kernel32.INSTANCE.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, false, processlist[i]);
            if (ph != null) {

                byte[] buffer = new byte[1024];
                com.sun.jna.platform.win32.Psapi.INSTANCE.GetModuleFileNameExA(ph, new WinNT.HANDLE(), buffer, buffer.length);
                String processName = Native.toString(buffer);


                //FIXME add multiple names support
                if (processName.contains(name)) {
                    pidsBS.add(processlist[i]);
                }
                com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(ph);
            }
        }
        return pidsBS;
    }

    /**
     * Enables debug privileges for this process, required for OpenProcess() to get
     * processes other than the current user
     *
     * @return {@code true} if debug privileges were successfully enabled.
     */
    private static boolean enableDebugPrivilege() {
        HANDLEByReference hToken = new HANDLEByReference();
        boolean success = Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),
                WinNT.TOKEN_QUERY | WinNT.TOKEN_ADJUST_PRIVILEGES, hToken);
        if (!success) {
            return false;
        }
        try {
            WinNT.LUID luid = new WinNT.LUID();
            success = Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid);
            if (!success) {
                return false;
            }
            WinNT.TOKEN_PRIVILEGES tkp = new WinNT.TOKEN_PRIVILEGES(1);
            tkp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new DWORD(WinNT.SE_PRIVILEGE_ENABLED));
            success = Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tkp, 0, null, null);
            int err = Native.getLastError();
            if (!success) {
                return false;
            } else if (err == WinError.ERROR_NOT_ALL_ASSIGNED) {
                return false;
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(hToken.getValue());
        }
        return true;
    }

    public static List<WindowInfo> findAllWindowsMatching(List<Integer> pids, List<String> titlesFilter) {
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
    }

    public static WindowInfo getWindowInfo(WinDef.HWND hWnd) {
        RECT r = new RECT();
        User32.INSTANCE.GetWindowRect(hWnd, r);
        char[] buffer = new char[1024];
        User32.INSTANCE.GetWindowText(hWnd, buffer, buffer.length);
        String title = Native.toString(buffer);
        WindowInfo info = new WindowInfo(r, title);
        return info;
    }

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = (User32) Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);

        int GetWindowText(WinDef.HWND hWnd, char[] lpString, int nMaxCount);

        int GetWindowRect(WinDef.HWND hWnd, RECT r);

        int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference pref);

    }

    public interface Psapi extends StdCallLibrary {
        Psapi INSTANCE = (Psapi) Native.load("Psapi", Psapi.class);

        boolean EnumProcesses(int[] ProcessIDsOut, int size, int[] BytesReturned);

        boolean EnumProcessModules(WinNT.HANDLE hProcess, WinDef.HMODULE[] lphModule, int cb, IntByReference lpcbNeeded);

        WinDef.DWORD GetModuleBaseNameW(Pointer hProcess, Pointer hModule, byte[] lpBaseName, int nSize);

        boolean GetModuleInformation(WinNT.HANDLE hProcess, WinDef.HMODULE hModule, com.sun.jna.platform.win32.Psapi.MODULEINFO lpmodinfo, int cb);

        int GetModuleFileNameExA(WinNT.HANDLE process, WinNT.HANDLE module, byte[] lpFilename, int nSize);
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = (Kernel32) Native.load("Kernel32", Kernel32.class);

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
