// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.util.Comparator;

class LenientDateTimeFormatComparator implements Comparator {
    /**
     * Protected so LenientDateTimeParserExpander can create one for sorting.
     */
    protected LenientDateTimeFormatComparator() {
    }

    /*
     * Compares expanded date/time format strings for the purposes of sorting
     * them in their original order.
     */
    @Override
    public int compare(final Object arg0, final Object arg1) {
        final int firstIndex = ((LenientDateTimeFormat) arg0).getCreationIndex();
        final int secondIndex = ((LenientDateTimeFormat) arg1).getCreationIndex();

        /*
         * First compare pattern length. Longer strings are preferred.
         */
        if (firstIndex < secondIndex) {
            return -1;
        } else if (firstIndex > secondIndex) {
            return 1;
        } else {
            return 0;
        }
    }
}
