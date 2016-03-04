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
public enum QueueOptions {

    /**
    * No queue options
    */
    NONE(0),
    /**
    * Create a plan Id for the build, do not run it
    */
    DO_NOT_RUN(1),
    ;

    private int value;

    private QueueOptions(final int value) {
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

        if (name.equals("DO_NOT_RUN")) { //$NON-NLS-1$
            return "doNotRun"; //$NON-NLS-1$
        }

        return null;
    }
}
