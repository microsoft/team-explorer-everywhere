// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Check;

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
     * This static initializer is a "best-effort" native code loader (no
     * exceptions thrown for normal load failures).
     *
     * Apps with multiple classloaders (like Eclipse) can run this initializer
     * more than once in a single JVM OS process, and on some platforms
     * (Windows) the native libraries will fail to load the second time, because
     * they're already loaded. This failure can be ignored because the native
     * code will execute fine.
     */
    static {
        NativeLoader.loadLibraryAndLogError(LibraryNames.WINDOWS_MESSAGEWINDOW_LIBRARY_NAME);
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

    private static native long nativeCreateWindow(long hwndParent, String className, String windowTitle, long userData);

    private static native boolean nativeDestroyWindow(long hwnd);

    private static native void nativeSendMessage(
        long hwnd,
        String className,
        long[] userData,
        int msg,
        long wParam,
        long lParam);
}
