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
public enum ScheduleDays {

    /**
    * Do not run.
    */
    NONE(0),
    /**
    * Run on Monday.
    */
    MONDAY(1),
    /**
    * Run on Tuesday.
    */
    TUESDAY(2),
    /**
    * Run on Wednesday.
    */
    WEDNESDAY(4),
    /**
    * Run on Thursday.
    */
    THURSDAY(8),
    /**
    * Run on Friday.
    */
    FRIDAY(16),
    /**
    * Run on Saturday.
    */
    SATURDAY(32),
    /**
    * Run on Sunday.
    */
    SUNDAY(64),
    /**
    * Run on all days of the week.
    */
    ALL(127),
    ;

    private int value;

    private ScheduleDays(final int value) {
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

        if (name.equals("MONDAY")) { //$NON-NLS-1$
            return "monday"; //$NON-NLS-1$
        }

        if (name.equals("TUESDAY")) { //$NON-NLS-1$
            return "tuesday"; //$NON-NLS-1$
        }

        if (name.equals("WEDNESDAY")) { //$NON-NLS-1$
            return "wednesday"; //$NON-NLS-1$
        }

        if (name.equals("THURSDAY")) { //$NON-NLS-1$
            return "thursday"; //$NON-NLS-1$
        }

        if (name.equals("FRIDAY")) { //$NON-NLS-1$
            return "friday"; //$NON-NLS-1$
        }

        if (name.equals("SATURDAY")) { //$NON-NLS-1$
            return "saturday"; //$NON-NLS-1$
        }

        if (name.equals("SUNDAY")) { //$NON-NLS-1$
            return "sunday"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        return null;
    }
}
