// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

import com.microsoft.tfs.util.Check;

public class FontHelper {
    public static FontData[] bold(final FontData[] fontData) {
        Check.notNull(fontData, "fontData"); //$NON-NLS-1$

        final FontData[] result = new FontData[fontData.length];

        for (int i = 0; i < fontData.length; i++) {
            result[i] = new FontData(fontData[i].getName(), fontData[i].getHeight(), fontData[i].getStyle() | SWT.BOLD);
        }

        return result;
    }

    public static FontData[] italicize(final FontData[] fontData) {
        Check.notNull(fontData, "fontData"); //$NON-NLS-1$

        final FontData[] result = new FontData[fontData.length];

        for (int i = 0; i < fontData.length; i++) {
            result[i] =
                new FontData(fontData[i].getName(), fontData[i].getHeight(), fontData[i].getStyle() | SWT.ITALIC);
        }

        return result;
    }

    public static int getHeight(final Font font) {
        if (font != null
            && font.getFontData() != null
            && font.getFontData().length > 0
            && font.getFontData()[0] != null) {
            return font.getFontData()[0].getHeight();
        }

        return 10;
    }
}
