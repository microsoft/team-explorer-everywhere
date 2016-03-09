// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * A custom layout specifically for use in laying out work item form "groups".
 * This layout lays its controls out in a single horizontal row.
 *
 * This layout makes the assumption that each child control has a FormGroupData
 * object set as its layout data.
 */
public class FormGroupLayout extends Layout {
    private static final Log log = LogFactory.getLog(FormGroupLayout.class);

    /**
     * marginWidth specifies the number of pixels of horizontal margin that will
     * be placed along the left and right edges of the layout.
     *
     * The default value is 5.
     */
    public int marginWidth = 5;

    /**
     * marginHeight specifies the number of pixels of vertical margin that will
     * be placed along the top and bottom edges of the layout.
     *
     * The default value is 5.
     */
    public int marginHeight = 5;

    /**
     * horizontalSpacing specifies the number of pixels between the right edge
     * of one cell and the left edge of its neighbouring cell to the right.
     *
     * The default value is 5.
     */
    public int horizontalSpacing = 5;

    @Override
    protected Point computeSize(final Composite composite, final int wHint, final int hHint, final boolean flushCache) {
        int width = wHint;
        int height = hHint;

        final Control[] children = composite.getChildren();

        Point[] childSizes = null;
        if (width == SWT.DEFAULT || height == SWT.DEFAULT) {
            childSizes = new Point[children.length];
            for (int i = 0; i < children.length; i++) {
                childSizes[i] = getSize(children[i], flushCache);

                if (log.isTraceEnabled()) {
                    final String messageFormat = "computing size, child {0} ({1}) preferred size: {2}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        Integer.toString(i),
                        children[i].getClass().getName(),
                        childSizes[i]);

                    log.trace(message);
                }
            }
        }

        if (width == SWT.DEFAULT) {
            width = 0;
            for (int i = 0; i < children.length; i++) {
                final FormGroupData data = (FormGroupData) children[i].getLayoutData();
                if (data.isWidthPercentage()) {
                    width += childSizes[i].x;
                } else {
                    width += data.getWidth();
                }
                width += horizontalSpacing;
            }

            width += (2 * marginWidth);
        }

        if (height == SWT.DEFAULT) {
            height = 0;
            for (int i = 0; i < children.length; i++) {
                height = Math.max(height, childSizes[i].y);
            }

            height += (2 * marginHeight);
        }

        final Point computedSize = new Point(width, height);

        if (log.isDebugEnabled()) {
            final String messageFormat = "computeSize({0},{1},{2},{3}): {4}"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                composite,
                Integer.toString(wHint),
                Integer.toString(hHint),
                flushCache,
                computedSize);

            log.debug(message);
        }

        return computedSize;
    }

    protected Point getSize(final Control control, final boolean flushCache) {
        return control.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
    }

    @Override
    protected void layout(final Composite composite, final boolean flushCache) {
        final Control[] children = composite.getChildren();
        final FormGroupData[] childData = new FormGroupData[children.length];

        for (int i = 0; i < children.length; i++) {
            childData[i] = (FormGroupData) children[i].getLayoutData();

            if (log.isTraceEnabled()) {
                final String messageFormat = "laying out, child {0} ({1}) layout data: {2}"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    Integer.toString(i),
                    children[i].getClass().getName(),
                    childData[i]);

                log.trace(message);
            }
        }

        final Rectangle clientArea = composite.getClientArea();

        if (log.isDebugEnabled()) {
            final String messageFormat =
                "laying out, client area of composite starts at ({0},{1}) and is of size ({2},{3})"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                Integer.toString(clientArea.x),
                Integer.toString(clientArea.y),
                Integer.toString(clientArea.width),
                Integer.toString(clientArea.height));

            log.debug(message);
        }

        int availableWidth = clientArea.width - (2 * marginWidth);
        if (children.length > 0) {
            availableWidth -= ((children.length - 1) * horizontalSpacing);
        }

        if (log.isDebugEnabled()) {
            final String messageFormat = "laying out, total width for controls: {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(availableWidth));
            log.debug(message);
        }

        int availableWidthAfterFixedWidthAllocated = availableWidth;
        int totalPercent = 0;

        for (int i = 0; i < children.length; i++) {
            final FormGroupData data = (FormGroupData) children[i].getLayoutData();
            if (data.isWidthPercentage()) {
                totalPercent += data.getWidth();
            } else {
                availableWidthAfterFixedWidthAllocated -= data.getWidth();
            }
        }

        if (availableWidthAfterFixedWidthAllocated < 0) {
            availableWidthAfterFixedWidthAllocated = 0;
        }

        if (log.isDebugEnabled()) {
            final String messageFormat = "laying out, width for percent specified controls: {0}"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, Integer.toString(availableWidthAfterFixedWidthAllocated));
            log.debug(message);
        }

        int curX = clientArea.x + marginWidth;

        for (int i = 0; i < children.length; i++) {
            final FormGroupData data = (FormGroupData) children[i].getLayoutData();

            int width = data.getWidth();

            if (data.isWidthPercentage()) {
                if (availableWidthAfterFixedWidthAllocated == 0) {
                    width = 50; // hardcoded minimum
                } else {
                    width = (int) (((float) (width * availableWidthAfterFixedWidthAllocated)) / ((float) totalPercent));
                }
            }

            if (totalPercent == 0 && i == children.length - 1) {
                width += availableWidthAfterFixedWidthAllocated;
            }

            final Rectangle boundingRectangle =
                new Rectangle(curX, clientArea.y + marginHeight, width, clientArea.height - marginHeight);

            children[i].setBounds(boundingRectangle);
            curX += width;
            curX += horizontalSpacing;

            if (log.isTraceEnabled()) {
                final String messageFormat =
                    "laying out, child {0} ({1}) set bounds starting at ({2},{3}) and of size ({4},{5})"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    Integer.toString(i),
                    children[i].getClass().getName(),
                    Integer.toString(boundingRectangle.x),
                    Integer.toString(boundingRectangle.y),
                    Integer.toString(boundingRectangle.width),
                    Integer.toString(boundingRectangle.height));

                log.trace(message);
            }
        }
    }
}
