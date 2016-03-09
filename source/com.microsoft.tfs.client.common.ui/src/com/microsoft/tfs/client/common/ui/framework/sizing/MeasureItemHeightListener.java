// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.sizing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * MeasureItemHeightListener allows one to control the height of items with
 * measurable item heights. One should attach it like:
 *
 * control.addListener(41, new MeasureItemHeightListener(padding));
 *
 * One should use the constant 41 instead of SWT.MeasureItem for backward
 * compatibility.
 *
 * You may pass an integer to the constructor which specifies the padding to add
 * to the items. This is useful, for example, to space out items in densely
 * populated tables.
 *
 * You may use the two-arg constructor to specify absolute heights.
 */
public class MeasureItemHeightListener implements Listener {
    private final int height;
    private final MeasureItemHeightListenerType mode;

    /* Compute the height the first time through the event handler */
    private int totalHeight = SWT.DEFAULT;

    /**
     * This will create a MeasureItem listener which adds the given height
     * difference to each measured item.
     *
     * @param heightDifference
     *        Pixels to add to the height of items
     */
    public MeasureItemHeightListener(final int heightDifference) {
        this(heightDifference, MeasureItemHeightListenerType.RELATIVE);
    }

    /**
     * This will create a MeasureItem listener which sets the absolute height of
     * items, or adds the given height difference.
     *
     * If mode is RELATIVE, this will add the given height to the item.
     *
     * If mode is ABSOLUTE, this will set the item height to the given height.
     *
     * @param height
     *        Number of pixels to set or add to the item height
     * @param mode
     *        ABSOLUTE or RELATIVE to determine how to handle the height
     *        argument
     */
    public MeasureItemHeightListener(final int height, final MeasureItemHeightListenerType mode) {
        this.height = height;
        this.mode = mode;
    }

    @Override
    public void handleEvent(final Event event) {
        /*
         * OS X does not properly handle changing event.height here.
         *
         * (Calling event.widget.redraw() the first time called will work for
         * the initial layout: if it's ever re-layed-out (ie, if the sorting
         * options are changed), then it will no longer pay attention to
         * event.height, causing it to return to the initial height.)
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return;
        }

        /* First time through, compute the new height */
        if (totalHeight == SWT.DEFAULT) {
            if (mode == MeasureItemHeightListenerType.RELATIVE) {
                totalHeight = event.height + height;
            } else {
                totalHeight = height;
            }
        }

        event.height = totalHeight;
    }

    static final class MeasureItemHeightListenerType extends TypesafeEnum {
        public static final MeasureItemHeightListenerType RELATIVE = new MeasureItemHeightListenerType(0);
        public static final MeasureItemHeightListenerType ABSOLUTE = new MeasureItemHeightListenerType(1);

        protected MeasureItemHeightListenerType(final int value) {
            super(value);
        }
    }
}
