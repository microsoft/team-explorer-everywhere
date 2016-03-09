// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class ArithmeticalOperators {
    private static final String[] strings;

    static {
        strings = new String[] {
            "+", //$NON-NLS-1$
            "-" //$NON-NLS-1$
        };
    }

    public static Arithmetic find(final String s) {
        int indexOf = -1;
        int currentPosition = 0;
        while (indexOf == -1 && currentPosition < strings.length) {
            if (strings[currentPosition].equals(s)) {
                indexOf = currentPosition;
            }
            ++currentPosition;
        }

        return Arithmetic.get(indexOf + 1);
    }

    public static String getString(final Arithmetic a) {
        return strings[a.getValue() - 1];
    }
}
