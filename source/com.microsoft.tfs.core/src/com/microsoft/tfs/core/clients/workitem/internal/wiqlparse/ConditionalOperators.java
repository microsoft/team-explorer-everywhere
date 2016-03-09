// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class ConditionalOperators {
    private static final String[] strings;

    static {
        strings = new String[] {
            "=", //$NON-NLS-1$
            "<>", //$NON-NLS-1$
            "<", //$NON-NLS-1$
            ">", //$NON-NLS-1$
            "<=", //$NON-NLS-1$
            ">=", //$NON-NLS-1$
            "under", //$NON-NLS-1$
            "in", //$NON-NLS-1$
            "contains", //$NON-NLS-1$
            "contains words", //$NON-NLS-1$
            "in group", //$NON-NLS-1$
            "==", //$NON-NLS-1$
            "!=" //$NON-NLS-1$
        };
    }

    public static Condition find(String s) {
        s = s.toLowerCase();

        int indexOf = -1;
        int currentPosition = 0;
        while (indexOf == -1 && currentPosition < strings.length) {
            if (strings[currentPosition].equals(s)) {
                indexOf = currentPosition;
            }
            ++currentPosition;
        }

        final Condition condition = Condition.get(indexOf + 1);

        if (condition == Condition.EQUALS_ALIAS) {
            return Condition.EQUALS;
        }

        if (condition == Condition.NOT_EQUALS_ALIAS) {
            return Condition.NOT_EQUALS;
        }

        return condition;
    }

    public static String getString(final Condition c) {
        return strings[c.getValue() - 1];
    }
}
