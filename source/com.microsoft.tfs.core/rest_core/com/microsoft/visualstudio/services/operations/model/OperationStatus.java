// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.operations.model;

/**
 *
 *
 * @threadsafety unknown
 */
public enum OperationStatus {
    /**
     * The operation object does not have the status set.
     */
    NOT_SET(0),

    /**
     * The operation has been queued.
     */
    QUEUED(1),

    /**
     * The operation is in progress.
     */
    IN_PROGRESS(2),

    /**
     * The operation was cancelled by the user.
     */
    CANCELLED(3),

    /**
     * The operation completed successfully.
     */
    SUCCEEDED(4),

    /**
     * The operation completed with a failure.
     */
    FAILED(5),;

    private OperationStatus(final int value) {
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("NOT_SET")) { //$NON-NLS-1$
            return "notSet"; //$NON-NLS-1$
        }

        if (name.equals("QUEUED")) { //$NON-NLS-1$
            return "queued"; //$NON-NLS-1$
        }

        if (name.equals("IN_PROGRESS")) { //$NON-NLS-1$
            return "inProgress"; //$NON-NLS-1$
        }

        if (name.equals("CANCELLED")) { //$NON-NLS-1$
            return "cancelled"; //$NON-NLS-1$
        }

        if (name.equals("SUCCEEDED")) { //$NON-NLS-1$
            return "succeeded"; //$NON-NLS-1$
        }

        if (name.equals("FAILED")) { //$NON-NLS-1$
            return "failed"; //$NON-NLS-1$
        }

        return null;
    }
}
