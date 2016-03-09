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
public enum PullRequestStatus {

    NOT_SET((byte) 0),
    ACTIVE((byte) 1),
    ABANDONED((byte) 2),
    COMPLETED((byte) 3),
    ;

    private byte value;

    private PullRequestStatus(final byte value) {
        this.value = value;
    }

    public byte getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("NOT_SET")) { //$NON-NLS-1$
            return "notSet"; //$NON-NLS-1$
        }

        if (name.equals("ACTIVE")) { //$NON-NLS-1$
            return "active"; //$NON-NLS-1$
        }

        if (name.equals("ABANDONED")) { //$NON-NLS-1$
            return "abandoned"; //$NON-NLS-1$
        }

        if (name.equals("COMPLETED")) { //$NON-NLS-1$
            return "completed"; //$NON-NLS-1$
        }

        return null;
    }
}
