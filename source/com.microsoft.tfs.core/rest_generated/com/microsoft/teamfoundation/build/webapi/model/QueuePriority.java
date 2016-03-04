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
public enum QueuePriority {

    /**
    * Low priority.
    */
    LOW(5),
    /**
    * Below normal priority.
    */
    BELOW_NORMAL(4),
    /**
    * Normal priority.
    */
    NORMAL(3),
    /**
    * Above normal priority.
    */
    ABOVE_NORMAL(2),
    /**
    * High priority.
    */
    HIGH(1),
    ;

    private int value;

    private QueuePriority(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("LOW")) { //$NON-NLS-1$
            return "low"; //$NON-NLS-1$
        }

        if (name.equals("BELOW_NORMAL")) { //$NON-NLS-1$
            return "belowNormal"; //$NON-NLS-1$
        }

        if (name.equals("NORMAL")) { //$NON-NLS-1$
            return "normal"; //$NON-NLS-1$
        }

        if (name.equals("ABOVE_NORMAL")) { //$NON-NLS-1$
            return "aboveNormal"; //$NON-NLS-1$
        }

        if (name.equals("HIGH")) { //$NON-NLS-1$
            return "high"; //$NON-NLS-1$
        }

        return null;
    }
}
