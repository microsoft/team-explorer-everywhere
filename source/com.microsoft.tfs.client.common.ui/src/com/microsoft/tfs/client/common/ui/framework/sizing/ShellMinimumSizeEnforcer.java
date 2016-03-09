// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.sizing;

import java.lang.reflect.Method;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;

/**
 * This class enforces a minimum size on a Shell. The enforcement is done in one
 * of two ways: -- Under Eclipse 3.1, the method shell.setMinimumSize is
 * reflectively called. This is the preferred method of minimum size
 * enforcement, since a native window system call is made that disallows the
 * user from sizing the Shell below the minimum size. -- When not under Eclipse
 * 3.1, the setMinimumSize method is unavailable. This class then registers a
 * ControlListener with the Shell, and when the Shell is resized below its
 * minimum the listener automatically resizes it to the minimum. This method is
 * not as clean from the user's perspective (try it and see the difference) but
 * it's all that's available pre-Eclipse 3.1.
 *
 * Additionally, if the system property "com.microsoft.tfs.ui.minsize-legacy" is
 * set to "true", the legacy Eclipse < 3.1 behavior will always be selected.
 *
 * It's suggested to use this class by overriding Dialog.getInitialSize() like:
 *
 * protected Point getInitialSize() { Point size = super.getInitialSize(); new
 * MinimumSizeEnforcer(getShell(), size.x, size.y); return size; } That way the
 * minimum size doesn't have to be hardcoded, and is determined at runtime by
 * SWT.
 */
public class ShellMinimumSizeEnforcer {
    private static boolean FORCE_LEGACY_BEHAVIOR = Boolean.getBoolean("com.microsoft.tfs.ui.minsize-legacy"); //$NON-NLS-1$

    private final Shell shell;
    private final int minimumWidth;
    private final int minimumHeight;
    private final Point currentLocation;

    /**
     * Create a new MinimumSizeEnforcer for the given shell.
     *
     * @param shell
     *        shell to enforce minimum size on
     * @param minimumWidth
     *        minimum width to allow
     * @param minimumHeight
     *        minimum height to allow
     */
    public ShellMinimumSizeEnforcer(final Shell shell, final int minimumWidth, final int minimumHeight) {
        this.shell = shell;
        this.minimumWidth = minimumWidth;
        this.minimumHeight = minimumHeight;
        currentLocation = shell.getLocation();

        /*
         * Note that legacy DOES NOT work on GTK, or presumably Motif. Altering
         * the size of a shell in its resize listener causes an event storm.
         */
        if ((FORCE_LEGACY_BEHAVIOR || !attemptSetMinimumSize())
            && !WindowSystem.isCurrentWindowSystem(WindowSystem.X_WINDOW_SYSTEM)) {
            final MinSizeListener listener = new MinSizeListener();
            shell.addControlListener(listener);
        }
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public int getMinimumHeight() {
        return minimumHeight;
    }

    // try to call the Eclipse 3.1 method. this is done reflectively so as
    // to still run under earlier eclipse versions. if any problems, we return
    // false and fall back to the listener mechanism.
    private boolean attemptSetMinimumSize() {
        try {
            final Method m = shell.getClass().getMethod("setMinimumSize", new Class[] //$NON-NLS-1$
            {
                Integer.TYPE,
                Integer.TYPE
            });
            m.invoke(shell, new Object[] {
                new Integer(minimumWidth),
                new Integer(minimumHeight)
            });
            return true;
        } catch (final Throwable t) {
            return false;
        }
    }

    /*
     * There's some complexity in the listener below, and this is an attempt to
     * explain it.
     *
     * The problem is that many resizes involve not only a resize but a move.
     * Consider resizing by dragging the upper left corner of a window down and
     * to the right. The result will be a smaller window, but one that has moved
     * as well. From Eclipse's perspective the location of a window is
     * determined by its upper left corner. So resizing a window by dragging
     * it's lower right corner up and to the left will *not* result in a move,
     * only a resize.
     *
     * We always want to "correct" resizes that size the window below the
     * minimum. However, an additional step of correction is needed for resizes
     * that also reposition the window.
     *
     * How do we know when we're in this case? Luckily Eclipse calls the
     * controlMoved listener before it calls the controlResized listener in this
     * case. So we check the size in both controlResized and controlMoved. If
     * the size has fallen below the minimum in controlMoved check, we also
     * correct the position of the window by the same offset we changed the size
     * by. In the controlResized check, we don't attempt to change the position
     * of the window. This (complicated) procedure handles all the cases.
     */

    private class MinSizeListener implements ControlListener {
        @Override
        public void controlResized(final ControlEvent e) {
            checkSize(false);
        }

        @Override
        public void controlMoved(final ControlEvent e) {
            checkSize(true);
        }

        private void checkSize(final boolean adjustLocation) {
            final Rectangle currentSize = shell.getBounds();
            final int currentHeight = currentSize.height;
            final int currentWidth = currentSize.width;

            int newHeight = currentSize.height;
            int newWidth = currentSize.width;

            int heightAdjustment = 0;
            int widthAdjustment = 0;

            if (currentHeight < minimumHeight) {
                newHeight = minimumHeight;
                heightAdjustment = currentHeight - minimumHeight;
            }

            if (currentWidth < minimumWidth) {
                newWidth = minimumWidth;
                widthAdjustment = currentWidth - minimumWidth;
            }

            if (newHeight != currentHeight || newWidth != currentWidth) {
                shell.setSize(newWidth, newHeight);
                if (adjustLocation) {
                    final Point currentLocation = shell.getLocation();
                    shell.setLocation(currentLocation.x + widthAdjustment, currentLocation.y + heightAdjustment);
                }
            }
        }
    }
}
