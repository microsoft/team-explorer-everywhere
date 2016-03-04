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
public enum GetOption {

    /**
    * Use the latest changeset at the time the build is queued.
    */
    LATEST_ON_QUEUE(0),
    /**
    * Use the latest changeset at the time the build is started.
    */
    LATEST_ON_BUILD(1),
    /**
    * A user-specified version has been supplied.
    */
    CUSTOM(2),
    ;

    private int value;

    private GetOption(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("LATEST_ON_QUEUE")) { //$NON-NLS-1$
            return "latestOnQueue"; //$NON-NLS-1$
        }

        if (name.equals("LATEST_ON_BUILD")) { //$NON-NLS-1$
            return "latestOnBuild"; //$NON-NLS-1$
        }

        if (name.equals("CUSTOM")) { //$NON-NLS-1$
            return "custom"; //$NON-NLS-1$
        }

        return null;
    }
}
