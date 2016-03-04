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
public enum DefinitionQueueStatus {

    /**
    * When enabled the definition queue allows builds to be queued by users, the system will queue scheduled, gated and continuous integration builds, and the queued builds will be started by the system.
    */
    ENABLED(0),
    /**
    * When paused the definition queue allows builds to be queued by users and the system will queue scheduled, gated and continuous integration builds. Builds in the queue will not be started by the system.
    */
    PAUSED(1),
    /**
    * When disabled the definition queue will not allow builds to be queued by users and the system will not queue scheduled, gated or continuous integration builds. Builds already in the queue will not be started by the system.
    */
    DISABLED(2),
    ;

    private int value;

    private DefinitionQueueStatus(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("ENABLED")) { //$NON-NLS-1$
            return "enabled"; //$NON-NLS-1$
        }

        if (name.equals("PAUSED")) { //$NON-NLS-1$
            return "paused"; //$NON-NLS-1$
        }

        if (name.equals("DISABLED")) { //$NON-NLS-1$
            return "disabled"; //$NON-NLS-1$
        }

        return null;
    }
}
