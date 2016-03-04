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
public enum ProcessTemplateType {

    /**
    * Indicates a custom template.
    */
    CUSTOM(0),
    /**
    * Indicates a default template.
    */
    DEFAULT(1),
    /**
    * Indicates an upgrade template.
    */
    UPGRADE(2),
    ;

    private int value;

    private ProcessTemplateType(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("CUSTOM")) { //$NON-NLS-1$
            return "custom"; //$NON-NLS-1$
        }

        if (name.equals("DEFAULT")) { //$NON-NLS-1$
            return "default"; //$NON-NLS-1$
        }

        if (name.equals("UPGRADE")) { //$NON-NLS-1$
            return "upgrade"; //$NON-NLS-1$
        }

        return null;
    }
}
