// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class Condition {
    public static final Condition NONE = new Condition("NONE", 0); //$NON-NLS-1$
    public static final Condition EQUALS = new Condition("EQUALS", 1); //$NON-NLS-1$
    public static final Condition NOT_EQUALS = new Condition("NOT_EQUALS", 2); //$NON-NLS-1$
    public static final Condition LESS = new Condition("LESS", 3); //$NON-NLS-1$
    public static final Condition GREATER = new Condition("GREATER", 4); //$NON-NLS-1$
    public static final Condition LESS_OR_EQUALS = new Condition("LESS_OR_EQUALS", 5); //$NON-NLS-1$
    public static final Condition GREATER_OR_EQUALS = new Condition("GREATER_OR_EQUALS", 6); //$NON-NLS-1$
    public static final Condition UNDER = new Condition("UNDER", 7); //$NON-NLS-1$
    public static final Condition IN = new Condition("IN", 8); //$NON-NLS-1$
    public static final Condition CONTAINS = new Condition("CONTAINS", 9); //$NON-NLS-1$
    public static final Condition CONTAINS_WORDS = new Condition("CONTAINS_WORDS", 10); //$NON-NLS-1$
    public static final Condition GROUP = new Condition("GROUP", 11); //$NON-NLS-1$
    public static final Condition EQUALS_ALIAS = new Condition("EQUALS_ALIAS", 12); //$NON-NLS-1$
    public static final Condition NOT_EQUALS_ALIAS = new Condition("NOT_EQUALS_ALIAS", 13); //$NON-NLS-1$

    public static Condition get(final int ix) {
        switch (ix) {
            case 0:
                return NONE;
            case 1:
                return EQUALS;
            case 2:
                return NOT_EQUALS;
            case 3:
                return LESS;
            case 4:
                return GREATER;
            case 5:
                return LESS_OR_EQUALS;
            case 6:
                return GREATER_OR_EQUALS;
            case 7:
                return UNDER;
            case 8:
                return IN;
            case 9:
                return CONTAINS;
            case 10:
                return CONTAINS_WORDS;
            case 11:
                return GROUP;
            case 12:
                return EQUALS_ALIAS;
            case 13:
                return NOT_EQUALS_ALIAS;
            default:
                throw new IllegalArgumentException(String.valueOf(ix));
        }
    }

    private final int value;
    private final String type;

    private Condition(final String type, final int value) {
        this.type = type;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return type;
    }
}
