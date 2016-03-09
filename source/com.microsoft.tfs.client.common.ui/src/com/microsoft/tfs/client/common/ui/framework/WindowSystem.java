// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework;

import org.eclipse.swt.SWT;

import com.microsoft.tfs.util.BitField;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * Contains static methods to classify the current window system. This is mostly
 * jacked from com.microsoft.tfs.util.Platform.
 *
 * This class is thread-safe.
 */
public class WindowSystem extends BitField {
    /**
     * The cached platform string.
     */
    private static String windowSystemString = null;

    /**
     * Caches the matching platforms to make subsequent calls faster.
     */
    private static WindowSystem matchingWindowSystems = null;

    /**
     * Gets whether the current window system "is" the given window system.
     * Multiple window systems may evaluate to true. For instance this method
     * returns true when running on a JVM on MacOS X if given either
     * {@link Platform#AQUA} or {@link Platform#CARBON}.
     *
     * @param windowSystem
     *        the window system to test for the existence of (not null).
     *        Multiple platforms may match on a given window system.
     * @return true if this is the current window system
     */
    public synchronized final static boolean isCurrentWindowSystem(final WindowSystem windowSystem) {
        Check.notNull(windowSystem, "windowSystem"); //$NON-NLS-1$

        if (matchingWindowSystems == null) {
            final String ws = getCurrentWindowSystemString();

            /* Windows platforms */
            if (ws.equals("win32")) //$NON-NLS-1$
            {
                matchingWindowSystems = combine(new WindowSystem[] {
                    WINDOWS,
                    WIN32
                });
            } else if (ws.equals("wpf")) //$NON-NLS-1$
            {
                matchingWindowSystems = combine(new WindowSystem[] {
                    WINDOWS,
                    WPF
                });
            } else if (ws.equals("gtk")) //$NON-NLS-1$
            {
                matchingWindowSystems = combine(new WindowSystem[] {
                    X_WINDOW_SYSTEM,
                    GTK
                });
            } else if (ws.equals("motif")) //$NON-NLS-1$
            {
                matchingWindowSystems = combine(new WindowSystem[] {
                    X_WINDOW_SYSTEM,
                    MOTIF
                });
            } else if (ws.equals("carbon")) //$NON-NLS-1$
            {
                matchingWindowSystems = combine(new WindowSystem[] {
                    AQUA,
                    CARBON
                });
            } else if (ws.equals("cocoa")) //$NON-NLS-1$
            {
                matchingWindowSystems = combine(new WindowSystem[] {
                    AQUA,
                    COCOA
                });
            } else {
                matchingWindowSystems = NONE;
            }
        }

        return matchingWindowSystems.contains(windowSystem);
    }

    /**
     * Gets the current platform string. Do not match against this string; it's
     * mostly here for when Platform does not define <b>any</b> matching
     * platforms and we must report an error so the user can report it to us.
     *
     * @return a string which you should not match against that describes the
     *         current plaform.
     */
    public synchronized final static String getCurrentWindowSystemString() {
        if (windowSystemString == null) {
            windowSystemString = SWT.getPlatform();
        }

        return windowSystemString;
    }

    public static WindowSystem combine(final WindowSystem[] changeTypes) {
        return new WindowSystem(BitField.combine(changeTypes));
    }

    public final static WindowSystem NONE = new WindowSystem(0, "NONE"); //$NON-NLS-1$

    /**
     * Any Microsoft Windows UI (Win32/WPF)
     */
    public final static WindowSystem WINDOWS = new WindowSystem(1, "WINDOWS"); //$NON-NLS-1$

    /**
     * Any X Window System (GTK/Motif).
     */
    public final static WindowSystem X_WINDOW_SYSTEM = new WindowSystem(2, "X_WINDOW_SYSTEM"); //$NON-NLS-1$

    /**
     * Apple Mac OS X with Aqua (Carbon/Cocoa)
     */
    public final static WindowSystem AQUA = new WindowSystem(4, "AQUA"); //$NON-NLS-1$

    public final static WindowSystem WIN32 = new WindowSystem(8, "WIN32"); //$NON-NLS-1$

    public final static WindowSystem WPF = new WindowSystem(16, "WPF"); //$NON-NLS-1$

    public final static WindowSystem GTK = new WindowSystem(32, "GTK"); //$NON-NLS-1$

    public final static WindowSystem MOTIF = new WindowSystem(64, "Motif"); //$NON-NLS-1$

    public final static WindowSystem CARBON = new WindowSystem(128, "CARBON"); //$NON-NLS-1$

    public final static WindowSystem COCOA = new WindowSystem(256, "COCOA"); //$NON-NLS-1$

    private WindowSystem(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private WindowSystem(final int flags) {
        super(flags);
    }

    public boolean containsAll(final WindowSystem other) {
        return containsAllInternal(other);
    }

    public boolean contains(final WindowSystem other) {
        return containsInternal(other);
    }

    public boolean containsAny(final WindowSystem other) {
        return containsAnyInternal(other);
    }

    public WindowSystem remove(final Platform other) {
        return new WindowSystem(removeInternal(other));
    }

    public WindowSystem retain(final Platform other) {
        return new WindowSystem(retainInternal(other));
    }

    public WindowSystem combine(final Platform other) {
        return new WindowSystem(combineInternal(other));
    }
}
