// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.util.BitField;

public class TableTreeHelper {
    public static String getPadding(final int level, final boolean hasChildren, final TableTreeHelperFlags flags) {
        /*
         * We pad with spaces, this is the number of spaces to add for sanity.
         */
        int paddingSpaces = 0;

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA)) {
            /* Six spaces for indentation */
            final int indentationSize = 6;

            /* Pad out based on the hierarchy level of this item */
            paddingSpaces = level * indentationSize;

            /*
             * Correct for the disclosure box that is not present for childless
             * items
             */
            if (!hasChildren) {
                paddingSpaces += 5;
            }
        } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            /*
             * Eight spaces for indentation, except when using a bold font, then
             * only 6
             */
            final int indentationSize = (flags.contains(TableTreeHelperFlags.FONT_BOLD)) ? 6 : 8;

            /* Pad out based on the hierarchy level of this item */
            paddingSpaces = level * indentationSize;

            /*
             * Correct for the disclosure box that is not present for childless
             * items
             */
            if (!hasChildren) {
                paddingSpaces += indentationSize;
            }
        } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.GTK)) {
            /*
             * GTK handles the disclosure box properly. Only add indentation
             * spaces based on the level.
             */
            final int indentationSize = (flags.contains(TableTreeHelperFlags.FONT_BOLD)) ? 6 : 8;
            paddingSpaces = level * indentationSize;
        }

        if (paddingSpaces == 0) {
            return ""; //$NON-NLS-1$
        }

        final StringBuffer padding = new StringBuffer(paddingSpaces);
        for (int i = 0; i < paddingSpaces; i++) {
            padding.append(' ');
        }
        return padding.toString();
    }

    public static class TableTreeHelperFlags extends BitField {
        public static final TableTreeHelperFlags NONE = new TableTreeHelperFlags(0);

        public static final TableTreeHelperFlags FONT_BOLD = new TableTreeHelperFlags(1);
        public static final TableTreeHelperFlags FONT_ITALIC = new TableTreeHelperFlags(2);

        private TableTreeHelperFlags(final int flags) {
            super(flags);
        }

        public boolean contains(final TableTreeHelperFlags other) {
            return containsInternal(other);
        }

        public TableTreeHelperFlags combine(final TableTreeHelperFlags other) {
            return new TableTreeHelperFlags(combineInternal(other));
        }
    }
}
