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
public enum TfvcVersionOption {

    NONE(0),
    PREVIOUS(1),
    USE_RENAME(2),
    ;

    private int value;

    private TfvcVersionOption(final int value) {
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

        if (name.equals("PREVIOUS")) { //$NON-NLS-1$
            return "previous"; //$NON-NLS-1$
        }

        if (name.equals("USE_RENAME")) { //$NON-NLS-1$
            return "useRename"; //$NON-NLS-1$
        }

        return null;
    }
}
