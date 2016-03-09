// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.printers;

import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.util.Check;

public class BasicPrinter {
    /**
     * Prints a single line of text the width of the display (minus 1) filled
     * with the given character.
     */
    public static void printSeparator(final Display display, final char c) {
        Check.notNull(display, "display"); //$NON-NLS-1$

        final StringBuffer sb = new StringBuffer();

        final int w = display.getWidth() - 1;
        for (int i = 0; i < w; i++) {
            sb.append(c);
        }

        display.printLine(sb.toString());
    }
}
