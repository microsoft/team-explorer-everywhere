// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.common.model;

public enum ProjectState {
    DELETING(2),
    NEW(0),
    WELL_FORMED(1),
    CREATE_PENDING(3),
    // Used for filtering.
    ALL(-1),
    // Used for updating projects.
    UNCHANGED(-2),
    // Used for the project history.
    DELETED(4);

    private ProjectState(final int value) {
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("DELETING")) { //$NON-NLS-1$
            return "deleting"; //$NON-NLS-1$
        }

        if (name.equals("NEW")) { //$NON-NLS-1$
            return "new"; //$NON-NLS-1$
        }

        if (name.equals("WELL_FORMED")) { //$NON-NLS-1$
            return "wellFormed"; //$NON-NLS-1$
        }

        if (name.equals("CREATE_PENDING")) { //$NON-NLS-1$
            return "createPending"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        if (name.equals("UNCHANGED")) { //$NON-NLS-1$
            return "unchanged"; //$NON-NLS-1$
        }

        if (name.equals("DELETED")) { //$NON-NLS-1$
            return "deleted"; //$NON-NLS-1$
        }

        return null;
    }
}
