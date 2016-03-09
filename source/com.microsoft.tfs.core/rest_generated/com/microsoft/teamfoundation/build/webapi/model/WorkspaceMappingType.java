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
public enum WorkspaceMappingType {

    /**
    * The path is mapped in the workspace.
    */
    MAP(0),
    /**
    * The path is cloaked in the workspace.
    */
    CLOAK(1),
    ;

    private int value;

    private WorkspaceMappingType(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("MAP")) { //$NON-NLS-1$
            return "map"; //$NON-NLS-1$
        }

        if (name.equals("CLOAK")) { //$NON-NLS-1$
            return "cloak"; //$NON-NLS-1$
        }

        return null;
    }
}
