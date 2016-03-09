// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.webapi.patch;

// See RFC 6902 - JSON Patch for more details.
// http://www.faqs.org/rfcs/rfc6902.html

public enum Operation {
    ADD(0),
    REMOVE(1),
    REPLACE(2),
    MOVE(3),
    COPY(4),
    TEST(5),
    ;

    private int value;

    private Operation(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("ADD")) { //$NON-NLS-1$
            return "add"; //$NON-NLS-1$
        }

        if (name.equals("REMOVE")) { //$NON-NLS-1$
            return "remove"; //$NON-NLS-1$
        }

        if (name.equals("REPLACE")) { //$NON-NLS-1$
            return "replace"; //$NON-NLS-1$
        }

        if (name.equals("MOVE")) { //$NON-NLS-1$
            return "move"; //$NON-NLS-1$
        }

        if (name.equals("COPY")) { //$NON-NLS-1$
            return "copy"; //$NON-NLS-1$
        }

        if (name.equals("TEST")) { //$NON-NLS-1$
            return "test"; //$NON-NLS-1$
        }

        return null;
    }
}