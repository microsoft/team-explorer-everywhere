// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.image;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * A class to assist with retrieving SWT system images. (For example, warning
 * icons, etc.)
 *
 * NOTE: DO NOT dispose system images.
 */
public class SystemImageHelper {
    /**
     * Return the <code>Image</code> to be used when displaying an error.
     *
     * @param shell
     *        the current shell
     * @return image the error image
     */
    public static Image getErrorImage(final Shell shell) {
        return getSystemImage(shell, SWT.ICON_ERROR);
    }

    /**
     * Return the <code>Image</code> to be used when displaying a warning.
     *
     * @param shell
     *        the current shell
     * @return image the warning image
     */
    public static Image getWarningImage(final Shell shell) {
        return getSystemImage(shell, SWT.ICON_WARNING);
    }

    /**
     * Return the <code>Image</code> to be used when displaying information.
     *
     * @param shell
     *        the current shell
     * @return image the information image
     */
    public static Image getInfoImage(final Shell shell) {
        return getSystemImage(shell, SWT.ICON_INFORMATION);
    }

    /**
     * Return the <code>Image</code> to be used when displaying a question. *
     *
     * @param shell
     *        the current shell
     * @return image the question image
     */
    public static Image getQuestionImage(final Shell shell) {
        return getSystemImage(shell, SWT.ICON_QUESTION);
    }

    /**
     * Get an <code>Image</code> from the provide SWT image constant. Do not
     * dispose this image.
     *
     * @param shell
     *        the current shell
     * @param imageID
     *        the SWT image constant
     * @return image the image
     */
    public static Image getSystemImage(final Shell shell, final int imageId) {
        final Display display = (shell == null) ? Display.getCurrent() : shell.getDisplay();

        final Image[] image = new Image[1];
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                image[0] = display.getSystemImage(imageId);
            }
        });

        return image[0];
    }
}
