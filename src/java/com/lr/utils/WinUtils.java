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

import static com.sun.jna.platform.win32.WinNT.PROCESS_QUERY_INFORMATION;
import static com.sun.jna.platform.win32.WinNT.PROCESS_VM_READ;

public class WinUtils {

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

    public static List<Integer> findPidsMatching(String name) {
        int[] processlist = new int[2048];
        int[] dummylist = new int[1024];
        List<Integer> pidsBS = new ArrayList<>(5);
        WinUtils.Psapi.INSTANCE.EnumProcesses(processlist, 2048, dummylist);

        //FIXME improve to better filter out
        for (int pid : processlist) {
            WinNT.HANDLE ph = com.sun.jna.platform.win32.Kernel32.INSTANCE.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, false, pid);
            if (ph != null) {

                byte[] buffer = new byte[1024];
                com.sun.jna.platform.win32.Psapi.INSTANCE.GetModuleFileNameExA(ph, new WinNT.HANDLE(), buffer, buffer.length);
                String processName = Native.toString(buffer);


                if (processName.contains(name)) {
                    pidsBS.add(pid);
                }
                com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(ph);
            }
        }
        return pidsBS;
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


    public static class WindowInfo {
        RECT rect;
        String title;

        public WindowInfo(RECT rect, String title) {
            this.rect = rect;
            this.title = title;
        }

        public RECT getRect() {
            return rect;
        }

        public String toString() {
            return String.format("(%d,%d)-(%d,%d) : \"%s\"", rect.left, rect.top, rect.right, rect.bottom, title);
        }
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
}
