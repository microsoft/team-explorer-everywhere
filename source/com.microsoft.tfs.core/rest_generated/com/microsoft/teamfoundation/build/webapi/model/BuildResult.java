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
public enum BuildResult {

    /**
    * No result
    */
    NONE(0),
    /**
    * The build completed successfully.
    */
    SUCCEEDED(2),
    /**
    * The build completed compilation successfully but had other errors.
    */
    PARTIALLY_SUCCEEDED(4),
    /**
    * The build completed unsuccessfully.
    */
    FAILED(8),
    /**
    * The build was canceled before starting.
    */
    CANCELED(32),
    ;

    private int value;

    private BuildResult(final int value) {
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

        if (name.equals("SUCCEEDED")) { //$NON-NLS-1$
            return "succeeded"; //$NON-NLS-1$
        }

        if (name.equals("PARTIALLY_SUCCEEDED")) { //$NON-NLS-1$
            return "partiallySucceeded"; //$NON-NLS-1$
        }

        if (name.equals("FAILED")) { //$NON-NLS-1$
            return "failed"; //$NON-NLS-1$
        }

        if (name.equals("CANCELED")) { //$NON-NLS-1$
            return "canceled"; //$NON-NLS-1$
        }

        return null;
    }
}
