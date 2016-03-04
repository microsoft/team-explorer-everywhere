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
public enum ServiceHostStatus {

    /**
    * The service host is currently connected and accepting commands.
    */
    ONLINE(1),
    /**
    * The service host is currently disconnected and not accepting commands.
    */
    OFFLINE(2),
    ;

    private int value;

    private ServiceHostStatus(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("ONLINE")) { //$NON-NLS-1$
            return "online"; //$NON-NLS-1$
        }

        if (name.equals("OFFLINE")) { //$NON-NLS-1$
            return "offline"; //$NON-NLS-1$
        }

        return null;
    }
}
