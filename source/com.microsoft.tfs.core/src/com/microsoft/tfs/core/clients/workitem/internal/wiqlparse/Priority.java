// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.text.MessageFormat;

public class Priority {
    public static final Priority OPERAND = new Priority("OPERAND", 0); //$NON-NLS-1$
    public static final Priority ADD_OPERATOR = new Priority("ADD_OPERATOR", 1); //$NON-NLS-1$
    public static final Priority CONDITIONAL_OPERATOR = new Priority("CONDITIONAL_OPERATOR", 2); //$NON-NLS-1$
    public static final Priority UNARY_BOOL_OPERATOR = new Priority("UNARY_BOOL_OPERATOR", 3); //$NON-NLS-1$
    public static final Priority AND_OPERATOR = new Priority("AND_OPERATOR", 4); //$NON-NLS-1$
    public static final Priority OR_OPERATOR = new Priority("OR_OPERATOR", 5); //$NON-NLS-1$
    public static final Priority COMMA_OPERATOR = new Priority("COMMA_OPERATOR", 6); //$NON-NLS-1$
    public static final Priority SELECT_OPERATOR = new Priority("SELECT_OPERATOR", 7); //$NON-NLS-1$

    private final String description;
    private final int value;

    private Priority(final String description, final int value) {
        this.description = description;
        this.value = value;
    }

    public boolean isGreaterThanOrEqualTo(final Priority other) {
        return value >= other.value;
    }

    public boolean isGreaterThan(final Priority other) {
        return value > other.value;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} ({1})", description, Integer.toString(value)); //$NON-NLS-1$
    }
}
