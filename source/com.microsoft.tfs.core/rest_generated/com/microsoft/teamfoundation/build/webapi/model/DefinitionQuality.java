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
public enum DefinitionQuality {

    DEFINITION(1),
    DRAFT(2),
    ;

    private int value;

    private DefinitionQuality(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("DEFINITION")) { //$NON-NLS-1$
            return "definition"; //$NON-NLS-1$
        }

        if (name.equals("DRAFT")) { //$NON-NLS-1$
            return "draft"; //$NON-NLS-1$
        }

        return null;
    }
}
