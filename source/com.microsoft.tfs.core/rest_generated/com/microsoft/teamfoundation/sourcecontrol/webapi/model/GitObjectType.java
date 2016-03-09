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
public enum GitObjectType {

    BAD(0),
    COMMIT(1),
    TREE(2),
    BLOB(3),
    TAG(4),
    EXT2(5),
    OFS_DELTA(6),
    REF_DELTA(7),
    ;

    private int value;

    private GitObjectType(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("BAD")) { //$NON-NLS-1$
            return "bad"; //$NON-NLS-1$
        }

        if (name.equals("COMMIT")) { //$NON-NLS-1$
            return "commit"; //$NON-NLS-1$
        }

        if (name.equals("TREE")) { //$NON-NLS-1$
            return "tree"; //$NON-NLS-1$
        }

        if (name.equals("BLOB")) { //$NON-NLS-1$
            return "blob"; //$NON-NLS-1$
        }

        if (name.equals("TAG")) { //$NON-NLS-1$
            return "tag"; //$NON-NLS-1$
        }

        if (name.equals("EXT2")) { //$NON-NLS-1$
            return "ext2"; //$NON-NLS-1$
        }

        if (name.equals("OFS_DELTA")) { //$NON-NLS-1$
            return "ofsDelta"; //$NON-NLS-1$
        }

        if (name.equals("REF_DELTA")) { //$NON-NLS-1$
            return "refDelta"; //$NON-NLS-1$
        }

        return null;
    }
}
