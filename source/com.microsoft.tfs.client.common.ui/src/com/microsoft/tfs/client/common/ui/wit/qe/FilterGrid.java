// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnection;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnectionType;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryGrouping;

public class FilterGrid {
    static int WHITE = 0x00ffffff;
    static int GRAY = 0x00999999;
    static int CLEAR = 0x00fdfdfd;

    public static ImageData createImageData(final QEQueryGrouping grping, final int height, final int row) {

        final int scanlinepad = 1;
        final int grpingwidth = 6;
        final int width = grping.getMaxDepth() * grpingwidth;
        final int[] imageData = new int[width * height];
        // byte[] byteData = new byte[imageData.length * 4];

        QEQueryConnection outerconnection = grping.getConnection(grping.getMaxDepth(), row);
        for (int i = grping.getMaxDepth(); i > 0; i--) {
            final QEQueryConnection connection = grping.getConnection(i, row);
            // Call Paint group inbox with type of closed NONE Node Type
            if (outerconnection.getType() == QEQueryConnectionType.DOWN) {
                paintGroupingBox(
                    imageData,
                    connection,
                    grpingwidth,
                    i,
                    grping.getMaxDepth() * grpingwidth,
                    height,
                    CLEAR,
                    GRAY,
                    1);
            } else if (outerconnection.getType() == QEQueryConnectionType.UP) {
                paintGroupingBox(
                    imageData,
                    connection,
                    grpingwidth,
                    i,
                    grping.getMaxDepth() * grpingwidth,
                    height,
                    CLEAR,
                    GRAY,
                    2);
            } else if (outerconnection.getType() == QEQueryConnectionType.ACROSS) {
                paintGroupingBox(
                    imageData,
                    connection,
                    grpingwidth,
                    i,
                    grping.getMaxDepth() * grpingwidth,
                    height,
                    CLEAR,
                    GRAY,
                    3);
            } else {
                paintGroupingBox(
                    imageData,
                    connection,
                    grpingwidth,
                    i,
                    grping.getMaxDepth() * grpingwidth,
                    height,
                    CLEAR,
                    GRAY,
                    0);
            }
            if (!(connection.getType() == QEQueryConnectionType.NONE)) {
                outerconnection = connection;
            }

        }

        ImageData data = null;
        // pad image hack for table proper display
        if (width < 24) {
            final int offset = 24 - width;
            final byte[] byteData = new byte[24 * height * 4];

            int inner = 0;
            int innloop = 0;
            final int clear = 0x00fdfdfd;
            // Make image bigger to mask viewer bug of not resizing images.
            for (int i = 0; i < byteData.length; i++) {
                if (i == 0 || i % 4 == 0) {
                    // check if in right side of generated image data has been
                    // reached if so copy color values from generated image data
                    // ************ <-new base ******XXXXXXX X's= width of
                    // origenal and location to copy
                    // ************ ******XXXXXXX
                    // ************ ******XXXXXXX <- new base with generated
                    // image data added
                    if (inner * 24 * 4 + offset * 4 - 1 < i) {
                        // TRGB (transparience, red , green blue)
                        byteData[i] = (byte) (imageData[innloop + inner * width] >> 24 & 0xff);
                        byteData[i + 1] = (byte) (imageData[innloop + inner * width] >> 16 & 0xff);
                        byteData[i + 2] = (byte) (imageData[innloop + inner * width] >> 8 & 0xff);
                        byteData[i + 3] = (byte) (imageData[innloop + inner * width] & 0xff);
                        innloop++;
                        if (innloop >= width) {
                            inner++;
                            innloop = 0;
                        }

                    }
                    // Else make it white
                    else {
                        byteData[i] = (byte) (clear >> 24 & 0xff);
                        byteData[i + 1] = (byte) (clear >> 16 & 0xff);
                        byteData[i + 2] = (byte) (clear >> 8 & 0xff);
                        byteData[i + 3] = (byte) (clear & 0xff);

                    }
                }
            }
            data = new ImageData(
                24,
                height,
                32,
                new PaletteData(0x00ff0000, 0x0000ff00, 0x000000ff),
                scanlinepad,
                byteData);

        } else {
            final byte[] byteData = new byte[imageData.length * 4];
            for (int i = 0; i < imageData.length; i++) {
                byteData[4 * i] = (byte) (imageData[i] >> 24 & 0xff);
                byteData[4 * i + 1] = (byte) (imageData[i] >> 16 & 0xff);
                byteData[4 * i + 2] = (byte) (imageData[i] >> 8 & 0xff);
                byteData[4 * i + 3] = (byte) (imageData[i] & 0xff);
            }
            data = new ImageData(
                width,
                height,
                32,
                new PaletteData(0x00ff0000, 0x0000ff00, 0x000000ff),
                scanlinepad,
                byteData);
        }
        return data;
    }

    public static Image createGroupImage(
        final Display display,
        final QEQueryGrouping grping,
        final int height,
        final int row) {
        final ImageData data = createImageData(grping, height, row);
        data.transparentPixel = CLEAR;
        final Image image = new Image(display, data);

        return image;
    }

    private static void paintGroupingBox(
        final int[] imagedata,
        final QEQueryConnection c,
        final int grpwidth,
        final int depthconnection,
        final int totalwidth,
        final int height,
        final int colorbackground,
        final int colorline,
        final int nestedtype) {
        final int skiplen = totalwidth - (grpwidth * depthconnection); // Depth
                                                                       // to
        // start at
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < grpwidth; j++) {
                if (c.getType() == QEQueryConnectionType.UP) {
                    // Draw Horizontal line
                    if (i == height - 1) {
                        if (j != 0) {
                            imagedata[i * totalwidth + (skiplen + j)] = colorline;
                        } else {
                            imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                        }
                    }
                    // Draw vertical line
                    else if (i < height && j == 1) {
                        imagedata[i * totalwidth + (skiplen + j)] = colorline;
                    }
                    // fill with white
                    else {
                        imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                    }
                }
                if (c.getType() == QEQueryConnectionType.DOWN) {
                    // Draw Horizontal line
                    if (i == 1) {
                        if (j != 0) {
                            imagedata[i * totalwidth + (skiplen + j)] = colorline;
                        } else {
                            imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                        }
                    }
                    // Draw vertical line
                    else if (i > 1 && j == 1) {
                        imagedata[i * totalwidth + (skiplen + j)] = colorline;
                    }
                    // fill with white
                    else {
                        imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                    }
                }
                if (c.getType() == QEQueryConnectionType.ACROSS) {
                    // draw vertical line
                    if (j == 1) {
                        imagedata[i * totalwidth + (skiplen + j)] = colorline;
                    }
                    // else fill with white
                    else {
                        imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                    }
                }
                // FILL Based on highest order parent type. UP DOWN or ARROSS
                // Up is one Down 2
                if (c.getType() == QEQueryConnectionType.NONE) {

                    if (nestedtype == 1) {
                        if (i == 1) {
                            imagedata[i * totalwidth + (skiplen + j)] = colorline;
                        } else {
                            imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                        }
                    } else if (nestedtype == 2) {
                        if (i == height - 1) {
                            imagedata[i * totalwidth + (skiplen + j)] = colorline;

                        } else {
                            imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                        }
                    } else if (nestedtype == 3) {
                        imagedata[i * totalwidth + (skiplen + j)] = colorbackground;
                    } else {
                        imagedata[i * totalwidth + (skiplen + j)] = CLEAR;
                    }
                }
            }
        }
    }

    public static Image createAstixImage(final Display display, final int height, final int width) {
        final Image image = new Image(display, width, height);
        final GC gc = new GC(image);
        // gc.drawLine(0,0,height, height);
        gc.drawText("*", 5, 2); //$NON-NLS-1$
        gc.dispose();

        return image;
    }
}
