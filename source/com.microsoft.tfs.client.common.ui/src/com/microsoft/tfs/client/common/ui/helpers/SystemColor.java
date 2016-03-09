// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.ColorUtils;
import com.microsoft.tfs.util.Check;

public class SystemColor {
    private static final Object lock = new Object();

    private static Color dimmedWidgetForegroundColor;

    /**
     * Returns a color to use as a "dimmed" widget foreground color (created as
     * the average between the foreground and background widget colors.) This
     * color MUST NOT be disposed.
     *
     * @return A "dimmed" widget foreground color (MUST NOT be disposed)
     */
    public static Color getDimmedWidgetForegroundColor(final Display display) {
        Check.notNull(display, "display"); //$NON-NLS-1$

        synchronized (lock) {
            if (dimmedWidgetForegroundColor == null) {
                /*
                 * On Win32, we can call GetSysColor(COLOR_GRAYTEXT), which
                 * respects the current theme settings (particularly important
                 * for high contrast mode.)
                 */
                if (WindowSystem.isCurrentWindowSystem(WindowSystem.WIN32)) {
                    dimmedWidgetForegroundColor = ColorUtils.getWin32SystemColor(display, "COLOR_GRAYTEXT"); //$NON-NLS-1$
                }

                /*
                 * On non-win32 platforms, simply select the average of the
                 * foreground and background colors (typically a gray.)
                 */
                if (dimmedWidgetForegroundColor == null) {
                    dimmedWidgetForegroundColor = ColorUtils.getAverageColor(
                        display,
                        display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND),
                        display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
                }
            }

            return dimmedWidgetForegroundColor;
        }
    }
}
