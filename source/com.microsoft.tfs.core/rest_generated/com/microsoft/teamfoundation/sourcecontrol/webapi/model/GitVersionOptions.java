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
public enum GitVersionOptions {

    /**
    * Not specified
    */
    NONE(0),
    /**
    * Commit that changed item prior to the current version
    */
    PREVIOUS_CHANGE(1),
    /**
    * First parent of commit (HEAD^)
    */
    FIRST_PARENT(2),
    ;

    private int value;

    private GitVersionOptions(final int value) {
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

        if (name.equals("PREVIOUS_CHANGE")) { //$NON-NLS-1$
            return "previousChange"; //$NON-NLS-1$
        }

        if (name.equals("FIRST_PARENT")) { //$NON-NLS-1$
            return "firstParent"; //$NON-NLS-1$
        }

        return null;
    }
}
