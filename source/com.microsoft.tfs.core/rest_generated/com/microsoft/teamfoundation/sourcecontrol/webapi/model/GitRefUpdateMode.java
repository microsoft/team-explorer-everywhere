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

package com.microsoft.teamfoundation.sourcecontrol.webapi.model;


/** 
 */
public enum GitRefUpdateMode {

    /**
    * Indicates the Git protocol model where any refs that can be updated will be updated, but any failures will not prevent other updates from succeeding.
    */
    BEST_EFFORT(0),
    /**
    * Indicates that all ref updates must succeed or none will succeed. All ref updates will be atomically written. If any failure is encountered, previously successful updates will be rolled back and the entire operation will fail.
    */
    ALL_OR_NONE(1),
    ;

    private int value;

    private GitRefUpdateMode(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("BEST_EFFORT")) { //$NON-NLS-1$
            return "bestEffort"; //$NON-NLS-1$
        }

        if (name.equals("ALL_OR_NONE")) { //$NON-NLS-1$
            return "allOrNone"; //$NON-NLS-1$
        }

        return null;
    }
}
