// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.build.webapi.model;


/** 
 */
public enum BuildStatus {

    /**
    * No status.
    */
    NONE(0),
    /**
    * The build is currently in progress.
    */
    IN_PROGRESS(1),
    /**
    * The build has completed.
    */
    COMPLETED(2),
    /**
    * The build is cancelling
    */
    CANCELLING(4),
    /**
    * The build is inactive in the queue.
    */
    POSTPONED(8),
    /**
    * The build has not yet started.
    */
    NOT_STARTED(32),
    /**
    * All status.
    */
    ALL(47),
    ;

    private int value;

    private BuildStatus(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("NONE")) { //$NON-NLS-1$
            return "none"; //$NON-NLS-1$
        }

        if (name.equals("IN_PROGRESS")) { //$NON-NLS-1$
            return "inProgress"; //$NON-NLS-1$
        }

        if (name.equals("COMPLETED")) { //$NON-NLS-1$
            return "completed"; //$NON-NLS-1$
        }

        if (name.equals("CANCELLING")) { //$NON-NLS-1$
            return "cancelling"; //$NON-NLS-1$
        }

        if (name.equals("POSTPONED")) { //$NON-NLS-1$
            return "postponed"; //$NON-NLS-1$
        }

        if (name.equals("NOT_STARTED")) { //$NON-NLS-1$
            return "notStarted"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        return null;
    }
}
