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
public enum VersionControlChangeType {

    NONE(0),
    ADD(1),
    EDIT(2),
    ENCODING(4),
    RENAME(8),
    DELETE(16),
    UNDELETE(32),
    BRANCH(64),
    MERGE(128),
    LOCK(256),
    ROLLBACK(512),
    SOURCE_RENAME(1024),
    TARGET_RENAME(2048),
    PROPERTY(4096),
    ALL(8191),
    ;

    private int value;

    private VersionControlChangeType(final int value) {
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

        if (name.equals("ADD")) { //$NON-NLS-1$
            return "add"; //$NON-NLS-1$
        }

        if (name.equals("EDIT")) { //$NON-NLS-1$
            return "edit"; //$NON-NLS-1$
        }

        if (name.equals("ENCODING")) { //$NON-NLS-1$
            return "encoding"; //$NON-NLS-1$
        }

        if (name.equals("RENAME")) { //$NON-NLS-1$
            return "rename"; //$NON-NLS-1$
        }

        if (name.equals("DELETE")) { //$NON-NLS-1$
            return "delete"; //$NON-NLS-1$
        }

        if (name.equals("UNDELETE")) { //$NON-NLS-1$
            return "undelete"; //$NON-NLS-1$
        }

        if (name.equals("BRANCH")) { //$NON-NLS-1$
            return "branch"; //$NON-NLS-1$
        }

        if (name.equals("MERGE")) { //$NON-NLS-1$
            return "merge"; //$NON-NLS-1$
        }

        if (name.equals("LOCK")) { //$NON-NLS-1$
            return "lock"; //$NON-NLS-1$
        }

        if (name.equals("ROLLBACK")) { //$NON-NLS-1$
            return "rollback"; //$NON-NLS-1$
        }

        if (name.equals("SOURCE_RENAME")) { //$NON-NLS-1$
            return "sourceRename"; //$NON-NLS-1$
        }

        if (name.equals("TARGET_RENAME")) { //$NON-NLS-1$
            return "targetRename"; //$NON-NLS-1$
        }

        if (name.equals("PROPERTY")) { //$NON-NLS-1$
            return "property"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        return null;
    }
}
