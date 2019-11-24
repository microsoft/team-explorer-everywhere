package com.microsoft.tfs.jni.internal.winapi;

import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({
    "style",
    "lpfnWndProc",
    "cbClsExtra",
    "cbWndExtra",
    "hInstance",
    "hIcon",
    "hCursor",
    "hbrBackground",
    "lpszMenuName",
    "lpszClassName"
}) public class WNDCLASSW extends Structure {
    public WinDef.UINT style;
    public WNDPROC lpfnWndProc;
    public int cbClsExtra;
    public int cbWndExtra;
    public WinDef.HINSTANCE hInstance;
    public WinDef.HICON hIcon;
    public WinDef.HCURSOR hCursor;
    public WinDef.HBRUSH hbrBackground;
    public WString lpszMenuName;
    public WString lpszClassName;
}
