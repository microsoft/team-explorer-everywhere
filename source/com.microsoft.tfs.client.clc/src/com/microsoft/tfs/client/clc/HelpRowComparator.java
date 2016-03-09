// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.text.Collator;
import java.util.Comparator;

import com.microsoft.tfs.util.CollatorFactory;

public final class HelpRowComparator implements Comparator<String[]> {
    private final Collator commandNameAndSyntaxCollator;

    public HelpRowComparator() {
        super();

        commandNameAndSyntaxCollator = CollatorFactory.getCaseInsensitiveCollator();
    }

    @Override
    public int compare(final String[] arg0, final String[] arg1) {
        /*
         * A row is an array of strings, and we just sort on each string we
         * find.
         */
        final String[] first = arg0;
        final String[] second = arg1;

        int ret;

        // Any null array causes the compare to end right away.
        if (first == null || second == null) {
            if (first != null) {
                return 1;
            }
            if (second != null) {
                return -1;
            }
            return 0;
        }

        // Any null command causes the compare to end right away.
        if (first[0] == null || second[0] == null) {
            if (first[0] != null) {
                return 1;
            }
            if (second[0] != null) {
                return -1;
            }
            return 0;
        }

        // Compare command names.
        ret = commandNameAndSyntaxCollator.compare(first[0], second[0]);
        if (ret != 0) {
            return ret;
        }

        // Any null syntax causes the compare to end right away.
        if (first[1] == null || second[0] == null) {
            if (first[1] != null) {
                return 1;
            }
            if (second[1] != null) {
                return -1;
            }
            return 0;
        }

        // Compare syntax.
        return commandNameAndSyntaxCollator.compare(first[1], second[1]);
    }
}
