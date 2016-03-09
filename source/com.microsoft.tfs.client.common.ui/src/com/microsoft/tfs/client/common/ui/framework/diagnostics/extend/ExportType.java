// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.extend;

public class ExportType {
    public static final ExportType NEVER = new ExportType("NEVER"); //$NON-NLS-1$
    public static final ExportType ALWAYS = new ExportType("ALWAYS"); //$NON-NLS-1$
    public static final ExportType OPTIONAL = new ExportType("OPTIONAL"); //$NON-NLS-1$

    public static final ExportType fromString(final String s) {
        if (s == null) {
            throw new IllegalArgumentException();
        }

        if (NEVER.type.equalsIgnoreCase(s)) {
            return NEVER;
        }
        if (ALWAYS.type.equalsIgnoreCase(s)) {
            return ALWAYS;
        }
        if (OPTIONAL.type.equalsIgnoreCase(s)) {
            return OPTIONAL;
        }

        throw new IllegalArgumentException(s);
    }

    private final String type;

    private ExportType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
