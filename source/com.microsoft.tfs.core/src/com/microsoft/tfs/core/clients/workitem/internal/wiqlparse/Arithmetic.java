// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class Arithmetic {
    public static final Arithmetic NONE = new Arithmetic("NONE", 0); //$NON-NLS-1$
    public static final Arithmetic ADD = new Arithmetic("ADD", 1); //$NON-NLS-1$
    public static final Arithmetic SUBTRACT = new Arithmetic("SUBTRACT", 2); //$NON-NLS-1$

    public static Arithmetic get(final int ix) {
        switch (ix) {
            case 0:
                return NONE;
            case 1:
                return ADD;
            case 2:
                return SUBTRACT;
            default:
                throw new IllegalArgumentException(String.valueOf(ix));
        }
    }

    private final int value;
    private final String type;

    private Arithmetic(final String type, final int value) {
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
