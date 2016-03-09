// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Scrollable;

import com.microsoft.tfs.util.Check;

/**
 * Helper classes common to all generic controls and composites.
 */
public class ControlHelper {
    /**
     * Computes the display (absolute) boundaries of this widget - that is, the
     * on-screen coordinates of the boundaries.
     *
     * See {@link Control#getBounds()}
     *
     * @param widget
     *        The widget to get the bounds for.
     * @return The boundaries in display coordinates
     */
    public static Rectangle getDisplayBounds(final Control widget) {
        Check.notNull(widget, "widget"); //$NON-NLS-1$

        final Rectangle bounds = widget.getBounds();

        final Point displayStart = widget.toDisplay(bounds.x, bounds.y);

        bounds.x = displayStart.x;
        bounds.y = displayStart.y;

        return bounds;
    }

    /**
     * Computes the display (absolute) display area of this widget - that is,
     * the on-screen coordinates of the display area available to receivers (not
     * covered by trimmings.)
     *
     * See {@link Scrollable#getClientArea()}
     *
     * @param widget
     *        The widget to get the client area for
     * @return The client area in display coordinates
     */
    public static Rectangle getDisplayClientArea(final Scrollable widget) {
        Check.notNull(widget, "widget"); //$NON-NLS-1$

        final Rectangle clientArea = widget.getClientArea();

        final Point displayStart = widget.toDisplay(clientArea.x, clientArea.y);

        clientArea.x = displayStart.x;
        clientArea.y = displayStart.y;

        return clientArea;
    }
}
