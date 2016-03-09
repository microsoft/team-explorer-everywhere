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
public enum DeleteOptions {

    /**
    * No data should be deleted. This value should not be used.
    */
    NONE(0),
    /**
    * The drop location should be deleted.
    */
    DROP_LOCATION(1),
    /**
    * The test results should be deleted.
    */
    TEST_RESULTS(2),
    /**
    * The version control label should be deleted.
    */
    LABEL(4),
    /**
    * The build should be deleted.
    */
    DETAILS(8),
    /**
    * Published symbols should be deleted.
    */
    SYMBOLS(16),
    /**
    * All data should be deleted.
    */
    ALL(31),
    ;

    private int value;

    private DeleteOptions(final int value) {
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

        if (name.equals("DROP_LOCATION")) { //$NON-NLS-1$
            return "dropLocation"; //$NON-NLS-1$
        }

        if (name.equals("TEST_RESULTS")) { //$NON-NLS-1$
            return "testResults"; //$NON-NLS-1$
        }

        if (name.equals("LABEL")) { //$NON-NLS-1$
            return "label"; //$NON-NLS-1$
        }

        if (name.equals("DETAILS")) { //$NON-NLS-1$
            return "details"; //$NON-NLS-1$
        }

        if (name.equals("SYMBOLS")) { //$NON-NLS-1$
            return "symbols"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        return null;
    }
}
