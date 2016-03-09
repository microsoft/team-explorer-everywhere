// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.account.model;

/**
 * Placeholder class
 *
 * Should convert to generated class in the future
 */
public enum AccountType {
    PERSONAL(0), ORGANIZATION(1);

    private int value;

    private AccountType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("PERSONAL")) { //$NON-NLS-1$
            return "personal"; //$NON-NLS-1$
        }

        if (name.equals("ORGANIZATION")) { //$NON-NLS-1$
            return "organization"; //$NON-NLS-1$
        }

        return null;
    }
}
