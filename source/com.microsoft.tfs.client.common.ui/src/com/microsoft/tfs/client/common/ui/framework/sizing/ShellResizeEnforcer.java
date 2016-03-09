// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.sizing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;

/**
 * This class enforces resizability on a shell to resizableDirections
 * (SWT.HORIZONTAL and/or SWT.VERTICAL.) If an attempt is made to resize in a
 * direction that is not in resizableDirections, it will be snapped back.
 *
 * This may look "odd" on platforms that have wireframe resizing and not opaque
 * resizing. (Probably Eclipse 3.1 on Windows, at least.)
 *
 * Note that this DOES NOT work on GTK, or presumably Motif. Altering the size
 * of a shell in its resize listener causes an event storm.
 */
public class ShellResizeEnforcer {
    private final Shell shell;
    private final Point initialSize;
    private final int resizableDirections;

    public ShellResizeEnforcer(final Shell shell, final int resizableDirections) {
        this.shell = shell;
        this.resizableDirections = resizableDirections;

        initialSize = shell.getSize();

        if (resizableDirections != (SWT.HORIZONTAL | SWT.VERTICAL)
            && !WindowSystem.isCurrentWindowSystem(WindowSystem.X_WINDOW_SYSTEM)) {
            shell.addControlListener(new ShellResizeControlListener());
        }
    }

    private class ShellResizeControlListener implements ControlListener {
        @Override
        public void controlMoved(final ControlEvent e) {
            checkSize();
        }

        @Override
        public void controlResized(final ControlEvent e) {
            checkSize();
        }

        private void checkSize() {
            final Point originalSize = shell.getSize();
            final Point newSize = new Point(originalSize.x, originalSize.y);

            // constrain x direction
            if ((resizableDirections & SWT.HORIZONTAL) != SWT.HORIZONTAL) {
                newSize.x = initialSize.x;
            }

            // constrain y direction
            if ((resizableDirections & SWT.VERTICAL) != SWT.VERTICAL) {
                newSize.y = initialSize.y;
            }

            if (originalSize.x == newSize.x && originalSize.y == newSize.y) {
                return;
            }

            /*
             * Unhook ourselves as a control listener, then rehook ourselves.
             * This should prevent controlMoved() getting called when we change
             * our own size. See Bug #1917
             */
            shell.removeControlListener(this);
            shell.setSize(newSize);
            shell.addControlListener(this);
        }
    }
}
