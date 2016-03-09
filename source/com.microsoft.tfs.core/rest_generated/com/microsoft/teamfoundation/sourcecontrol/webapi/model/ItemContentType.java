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
public enum ItemContentType {

    RAW_TEXT(0),
    BASE64_ENCODED(1),
    ;

    private int value;

    private ItemContentType(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("RAW_TEXT")) { //$NON-NLS-1$
            return "rawText"; //$NON-NLS-1$
        }

        if (name.equals("BASE64_ENCODED")) { //$NON-NLS-1$
            return "base64Encoded"; //$NON-NLS-1$
        }

        return null;
    }
}
