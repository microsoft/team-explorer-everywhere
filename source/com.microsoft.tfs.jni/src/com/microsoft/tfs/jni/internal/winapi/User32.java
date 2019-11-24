package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.win32.W32APIOptions;

public interface User32 extends com.sun.jna.platform.win32.User32 {
    User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

    int GWLP_USERDATA = -21;

    boolean GetClassInfoW(HINSTANCE hInstance, WString lpClassName, WNDCLASSW lpWndClass);
    short RegisterClassW(WNDCLASSW lpWndClass);

    boolean PostMessageW(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
}
