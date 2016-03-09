// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.account.model;

/**
 * Placeholder class
 *
 * Should convert to generated class in the future
 */
public enum AccountStatus {

    NONE(0),
    /**
     * This hosting account is active and assigned to a customer.
     */
    ENABLED(1),
    /**
     * This hosting account is disabled.
     */
    DISABLED(2),
    /**
     * This account is part of deletion batch and scheduled for deletion.
     */
    DELETED(3);

    private int value;

    private AccountStatus(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("NONE")) { //$NON-NLS-1$
            return "none"; //$NON-NLS-1$
        }

        if (name.equals("ENABLED")) { //$NON-NLS-1$
            return "enabled"; //$NON-NLS-1$
        }

        if (name.equals("DISABLED")) { //$NON-NLS-1$
            return "disabled"; //$NON-NLS-1$
        }

        if (name.equals("DELETED")) { //$NON-NLS-1$
            return "deleted"; //$NON-NLS-1$
        }

        return null;
    }
}
