// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.layout;

import java.lang.reflect.Field;

import org.eclipse.swt.layout.GridLayout;

/**
 * The purpose of this class is to use reflection to set public fields of
 * GridLayout that are only available in Eclipse 3.1 and greater.
 *
 * This allows code to set these fields and still be compilable under Eclipse
 * 3.0, which is needed to support RAD. At runtime the fields will be
 * dynamically set if they exist. If they do not exist, no error will occur.
 */
public class GridLayoutMarginHelper {
    public static void setMargins(
        final GridLayout gridLayout,
        final int marginLeft,
        final int marginRight,
        final int marginTop,
        final int marginBottom) {
        setMarginLeft(gridLayout, marginLeft);
        setMarginRight(gridLayout, marginRight);
        setMarginTop(gridLayout, marginTop);
        setMarginBottom(gridLayout, marginBottom);
    }

    public static void setMarginLeft(final GridLayout gridLayout, final int marginLeft) {
        setField(gridLayout, "marginLeft", marginLeft); //$NON-NLS-1$
    }

    public static void setMarginRight(final GridLayout gridLayout, final int marginRight) {
        setField(gridLayout, "marginRight", marginRight); //$NON-NLS-1$
    }

    public static void setMarginTop(final GridLayout gridLayout, final int marginTop) {
        setField(gridLayout, "marginTop", marginTop); //$NON-NLS-1$
    }

    public static void setMarginBottom(final GridLayout gridLayout, final int marginBottom) {
        setField(gridLayout, "marginBottom", marginBottom); //$NON-NLS-1$
    }

    private static void setField(final Object obj, final String fieldName, final int value) {
        try {
            final Field field = obj.getClass().getField(fieldName);
            field.set(obj, new Integer(value));
        } catch (final Throwable t) {
            // ignore
        }
    }
}
