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
public enum VersionControlRecursionType {

    NONE(0),
    ONE_LEVEL(1),
    FULL(120),
    ;

    private int value;

    private VersionControlRecursionType(final int value) {
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

        if (name.equals("ONE_LEVEL")) { //$NON-NLS-1$
            return "oneLevel"; //$NON-NLS-1$
        }

        if (name.equals("FULL")) { //$NON-NLS-1$
            return "full"; //$NON-NLS-1$
        }

        return null;
    }
}
