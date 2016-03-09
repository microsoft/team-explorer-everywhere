// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.settings;

import java.text.MessageFormat;

import com.microsoft.tfs.util.TypesafeEnum;

public class Area extends TypesafeEnum {
    public static final Area FILE = new Area(0);
    public static final Area PROJECT = new Area(1);
    public static final Area WORKSPACE = new Area(2);

    private Area(final int value) {
        super(value);
    }

    public static Area fromValue(final int value) {
        if (value == FILE.getValue()) {
            return FILE;
        } else if (value == PROJECT.getValue()) {
            return PROJECT;
        } else if (value == WORKSPACE.getValue()) {
            return WORKSPACE;
        }

        final String messageFormat = "Value {0} does not map to an Area"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(value));
        throw new IllegalArgumentException(message);
    }
}
