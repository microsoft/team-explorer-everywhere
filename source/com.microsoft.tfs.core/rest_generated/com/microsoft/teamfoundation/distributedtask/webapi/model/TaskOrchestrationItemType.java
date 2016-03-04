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

package com.microsoft.teamfoundation.distributedtask.webapi.model;


/** 
 */
public enum TaskOrchestrationItemType {

    CONTAINER(0),
    JOB(1),
    ;

    private int value;

    private TaskOrchestrationItemType(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("CONTAINER")) { //$NON-NLS-1$
            return "container"; //$NON-NLS-1$
        }

        if (name.equals("JOB")) { //$NON-NLS-1$
            return "job"; //$NON-NLS-1$
        }

        return null;
    }
}
