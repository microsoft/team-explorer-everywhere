// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.image;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.util.Check;

public class ImageUtils {
    private static final Log log = LogFactory.getLog(ImageUtils.class);

    private ImageUtils() {
    }

    /**
     * Creates a "disclosure" or "drop-down" triangle suitable for the given
     * Control's font size and colors. The returned {@link Image} must be
     * disposed.
     *
     * @param control
     *        The control to use as the basis for creating a triangle
     * @return An {@link Image} representing a triangle
     */
    public static Image createDisclosureTriangle(final Control control) {
        Check.notNull(control, "control"); //$NON-NLS-1$

        GC controlGC = null;
        GC arrowGC = null;
        Image arrow = null;

        try {
            controlGC = new GC(control);
            controlGC.setFont(control.getFont());

            final int arrowHeight = controlGC.getFontMetrics().getHeight();
            final int arrowWidth = arrowHeight / 2;

            arrow = new Image(control.getDisplay(), arrowWidth, arrowHeight);

            arrowGC = new GC(arrow);
            arrowGC.setBackground(control.getBackground());
            arrowGC.fillRectangle(0, 0, arrowWidth, arrowHeight);

            arrowGC.setBackground(control.getForeground());
            arrowGC.fillPolygon(new int[] {
                0,
                arrowHeight / 2,
                arrowWidth,
                arrowHeight / 2,
                arrowWidth / 2,
                (int) ((((double) arrowHeight / 2) + ((double) arrowWidth / 2))),
            });

            /*
             * Some Eclipse / JFace / etc tools try to create a
             * "disabled version" of an image (ie, new Image(Display, Image,
             * Image.DISABLED);
             *
             * This is often a disaster.
             *
             * We need to set the background color to be transparent explicitly.
             * This involves creating a new Image based on the current arrow's
             * imagedata.
             */

            final ImageData arrowImageData = arrow.getImageData();
            arrowImageData.transparentPixel = arrowImageData.palette.getPixel(control.getBackground().getRGB());

            arrow.dispose();
            arrow = new Image(control.getDisplay(), arrowImageData);

            return arrow;
        } catch (final Exception e) {
            log.warn("Could not create drop hyperlink image", e); //$NON-NLS-1$

            if (arrow != null) {
                arrow.dispose();
            }

            /*
             * Note: do not dispose this imagehelper. Callers should dispose the
             * image manually.
             */
            return new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID).getImage("/images/common/drop_arrow.png"); //$NON-NLS-1$
        } finally {
            if (controlGC != null) {
                controlGC.dispose();
            }

            if (arrowGC != null) {
                arrowGC.dispose();
            }
        }
    }

    public static Image createRectangular(final Control control, final Color color, final int width, final int height) {
        Check.notNull(control, "control"); //$NON-NLS-1$
        Check.notNull(color, "color"); //$NON-NLS-1$

        final Image image = new Image(control.getDisplay(), width, height);
        final GC controlGC = new GC(image);

        if (controlGC != null) {
            controlGC.setBackground(color);
            controlGC.fillRectangle(0, 0, width, height);
            controlGC.dispose();
        }

        return image;
    }

    public static Image grayScaleImage(final Image image, final int width, final int height) {
        final ImageData data = image.getImageData();
        final Image scaleImage = new Image(image.getDevice(), data.scaledTo(width, height));
        return new Image(image.getDevice(), scaleImage, SWT.IMAGE_GRAY);
    }
}
