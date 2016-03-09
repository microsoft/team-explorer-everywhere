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
public enum ControllerStatus {

    /**
    * Indicates that the build controller cannot be contacted.
    */
    UNAVAILABLE(0),
    /**
    * Indicates that the build controller is currently available.
    */
    AVAILABLE(1),
    /**
    * Indicates that the build controller has taken itself offline.
    */
    OFFLINE(2),
    ;

    private int value;

    private ControllerStatus(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("UNAVAILABLE")) { //$NON-NLS-1$
            return "unavailable"; //$NON-NLS-1$
        }

        if (name.equals("AVAILABLE")) { //$NON-NLS-1$
            return "available"; //$NON-NLS-1$
        }

        if (name.equals("OFFLINE")) { //$NON-NLS-1$
            return "offline"; //$NON-NLS-1$
        }

        return null;
    }
}
