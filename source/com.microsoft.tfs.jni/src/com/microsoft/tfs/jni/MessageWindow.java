// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.winapi.Kernel32;
import com.microsoft.tfs.jni.internal.winapi.User32;
import com.microsoft.tfs.jni.internal.winapi.WNDCLASSW;
import com.microsoft.tfs.util.Check;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;

import java.util.HashMap;
import java.util.Map;

/**
 * A hidden Win32 window useful for interprocess communication via window
 * messages. Must be used on the UI thread.
 *
 * Only works on Windows platforms.
 */
public class MessageWindow {
    /**
     * Handles messages received by a {@link MessageWindow}.
     */
    public interface MessageListener {
        /**
         * Handles a single message. Always called on the UI thread.
         *
         * @param msg
         *        the user-defined message number (cast to <b>unsigned</b>
         *        internally)
         * @param wParam
         *        the wParam (cast to <b>unsigned</b> internally; restricted to
         *        32-bit range on 32-bit platforms)
         * @param lParam
         *        the lParam (restricted to 32-bit range on 32-bit platforms)
         */
        void messageReceived(int msg, long wParam, long lParam);
    }

    /**
     * Maps the window handles we've allocated to instances so we can route
     * messages received by static methods back to an instance.
     */
    private static Map<Long, MessageWindow> instances = new HashMap<Long, MessageWindow>();

    private final MessageListener listener;
    private long hwnd = 0;

    /**
     * Constructs a {@link MessageWindow}.
     *
     * @param hwndParent
     *        the parent window's handle (may be 0)
     * @param className
     *        the class name to attach to the window (must not be
     *        <code>null</code> or empty, length <= 256 chars)
     * @param windowTitle
     *        the window title (may be <code>null</code> or empty)
     * @param userData
     *        data to be set as <code>GWLP_USERDATA</code> on the window
     *        (restricted to 32-bit range on 32-bit platforms)
     * @param listener
     *        a listener invoked when messages are received by this
     *        {@link MessageWindow} (must not be <code>null</code>)
     */
    public MessageWindow(
        final long hwndParent,
        final String className,
        final String windowTitle,
        final long userData,
        final MessageListener listener) {
        Check.notNull(className, "className"); //$NON-NLS-1$
        Check.isTrue(className.length() <= 256, "the className must be <= 256 characters"); //$NON-NLS-1$
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        this.listener = listener;

        hwnd = nativeCreateWindow(hwndParent, className, windowTitle, userData);
        instances.put(hwnd, this);
    }

    /**
     * Destroys this message window. Do not call other methods after calling
     * this method.
     */
    public void destroyWindow() {
        if (hwnd != 0) {
            instances.remove(hwnd);
            nativeDestroyWindow(hwnd);
            hwnd = 0;
        }
    }

    /**
     * Sends a message to all windows with the specified class name.
     *
     * @param className
     *        the class of windows to send the message to (must not be
     *        <code>null</code> or empty)
     * @param userData
     *        an array of values that a window's user data must match one of be
     *        sent the message (pass <code>null</code> or empty to match all
     *        windows)
     * @param msg
     *        the message number to send (cast to <b>unsigned</b> internally)
     * @param wParam
     *        the wParam (cast to <b>unsigned</b> internally; restricted to
     *        32-bit range on 32-bit platforms)
     * @param lParam
     *        the lParam (restricted to 32-bit range on 32-bit platforms)
     * @throws RuntimeException
     *         if an error occurred sending the message
     */
    public void sendMessage(
        final String className,
        final long[] userData,
        final int msg,
        final long wParam,
        final long lParam) {
        Check.notNullOrEmpty(className, "className"); //$NON-NLS-1$
        Check.isTrue(hwnd != 0, "the message window has been destroyed"); //$NON-NLS-1$

        nativeSendMessage(hwnd, className, userData, msg, wParam, lParam);
    }

    /**
     * Called by native code when a notification is received.
     *
     * This method name must match the name expected by the native code.
     */
    private static void messageReceived(final long hwnd, final int msg, final long wParam, final long lParam) {
        if (hwnd != 0) {
            // Find the right instance
            final MessageWindow instance = instances.get(hwnd);
            if (instance != null) {
                instance.listener.messageReceived(msg, wParam, lParam);
            }
        }
    }

    private final static Kernel32 kernel32 = Kernel32.INSTANCE;
    private final static User32 user32 = User32.INSTANCE;

    private static WinDef.LRESULT wndProc(
        WinDef.HWND hwnd,
        WinDef.UINT uMsg,
        WinDef.WPARAM wParam,
        WinDef.LPARAM lParam) {
        int msg = uMsg.intValue();
        if (msg >= WinUser.WM_USER && msg <= 0x7FFFL) {
            messageReceived(Pointer.nativeValue(hwnd.getPointer()), msg, wParam.longValue(), lParam.longValue());
        }

        return user32.DefWindowProc(hwnd, msg, wParam, lParam);
    }

    private static long nativeCreateWindow(long hwndParent, String className, String windowTitle, long userData) {
        // Register the custom class if needed:
        WNDCLASSW wc = new WNDCLASSW();
        if (!user32.GetClassInfoW(kernel32.GetModuleHandle(null), new WString(className), wc)) {
            wc.style = new WinDef.UINT(0L);
            wc.lpfnWndProc = MessageWindow::wndProc;
            wc.cbClsExtra = 0;
            wc.cbWndExtra = 0;
            wc.hInstance = kernel32.GetModuleHandle(null);
            wc.hIcon = null;
            wc.hCursor = null;
            wc.hbrBackground = null;
            wc.lpszMenuName = null;
            wc.lpszClassName = new WString(className);

            if (user32.RegisterClassW(wc) == 0) {
                throw new RuntimeException("Error registering window class");
            }
        }

        WinDef.HWND hwnd = user32.CreateWindowEx(
            0,
            className,
            windowTitle,
            WinUser.WS_OVERLAPPED,
            0,
            0,
            0,
            0,
            new WinDef.HWND(Pointer.createConstant(hwndParent)),
            null,
            kernel32.GetModuleHandle(null),
            null);
        if (hwnd == null) {
            throw new RuntimeException("Error creating native message window");
        }

        user32.SetWindowLongPtr(hwnd, User32.GWLP_USERDATA, Pointer.createConstant(userData));

        return Pointer.nativeValue(hwnd.getPointer());
    }

    private static boolean nativeDestroyWindow(long hwnd) {
        return user32.DestroyWindow(new WinDef.HWND(Pointer.createConstant(hwnd)));
    }

    private static void nativeSendMessage(
        long hwnd,
        String className,
        long[] userData,
        int msg,
        long wParam,
        long lParam) {
        if (!user32.EnumWindows((windowHwnd, pointer) -> {
            // Ensure the window's user data matches:
            if (userData != null && userData.length > 0) {
                boolean match = false;
                BaseTSD.LONG_PTR windowUserData = user32.GetWindowLongPtr(windowHwnd, User32.GWLP_USERDATA);
                for (long userDataItem : userData) {
                    if (userDataItem == windowUserData.longValue()) {
                        match = true;
                        break;
                    }
                }

                if (!match) {
                    // Not an error, keep processing other windows.
                    return true;
                }
            }

            // Docs for WNDCLASS/WNDCLASSEX structs say 256 is the max class name length.
            char[] windowClassName = new char[256];

            // Test the class name for a match:

            if (user32.GetClassName(windowHwnd, windowClassName, windowClassName.length) == 0) {
                // Don't throw an exception, return true to keep the send process going:
                return true;
            }

            if (className.equals(new String(windowClassName))) {
                if (!user32.PostMessageW(
                    windowHwnd,
                    new WinDef.UINT(msg),
                    new WinDef.WPARAM(wParam),
                    new WinDef.LPARAM(lParam))) {
                    int error = kernel32.GetLastError();

                    // ERROR_ACCESS_DENIED is expected if User Interface Privilege Isolation (UIPI) blocked it.
                    // ERROR_NOT_ENOUGH_QUOTA is expected if we hit a configured message limit.
                    if (error != WinNT.ERROR_SUCCESS
                        && error != WinNT.ERROR_ACCESS_DENIED
                        && error != WinNT.ERROR_NOT_ENOUGH_QUOTA) {
                        // Set the error code our enumerator can report via exception:
                        kernel32.SetLastError(error);
                        return false;
                    }
                }
            }

            return true;
        }, null)) {
            throw new RuntimeException("PostMessage failed");
        }
    }
}
