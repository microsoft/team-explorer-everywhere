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
public enum TimelineRecordState {

    PENDING(0),
    IN_PROGRESS(1),
    COMPLETED(2),
    ;

    private int value;

    private TimelineRecordState(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("PENDING")) { //$NON-NLS-1$
            return "pending"; //$NON-NLS-1$
        }

        if (name.equals("IN_PROGRESS")) { //$NON-NLS-1$
            return "inProgress"; //$NON-NLS-1$
        }

        if (name.equals("COMPLETED")) { //$NON-NLS-1$
            return "completed"; //$NON-NLS-1$
        }

        return null;
    }
}
