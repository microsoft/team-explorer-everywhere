package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Callback;
import com.sun.jna.platform.win32.WinDef;

public interface WNDPROC extends Callback {
    WinDef.LRESULT callback(WinDef.HWND hwnd, WinDef.UINT uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
}
