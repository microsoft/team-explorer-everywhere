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
public enum GitRepositoryPermissions {

    NONE(0),
    ADMINISTER(1),
    GENERIC_READ(2),
    GENERIC_CONTRIBUTE(4),
    FORCE_PUSH(8),
    CREATE_BRANCH(16),
    CREATE_TAG(32),
    MANAGE_NOTE(64),
    POLICY_EXEMPT(128),
    /**
    * This defines the set of bits that are valid for the git permission space. When reading or writing git permissions, these are the only bits paid attention too.
    */
    ALL(255),
    BRANCH_LEVEL_PERMISSIONS(141),
    ;

    private int value;

    private GitRepositoryPermissions(final int value) {
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

        if (name.equals("ADMINISTER")) { //$NON-NLS-1$
            return "administer"; //$NON-NLS-1$
        }

        if (name.equals("GENERIC_READ")) { //$NON-NLS-1$
            return "genericRead"; //$NON-NLS-1$
        }

        if (name.equals("GENERIC_CONTRIBUTE")) { //$NON-NLS-1$
            return "genericContribute"; //$NON-NLS-1$
        }

        if (name.equals("FORCE_PUSH")) { //$NON-NLS-1$
            return "forcePush"; //$NON-NLS-1$
        }

        if (name.equals("CREATE_BRANCH")) { //$NON-NLS-1$
            return "createBranch"; //$NON-NLS-1$
        }

        if (name.equals("CREATE_TAG")) { //$NON-NLS-1$
            return "createTag"; //$NON-NLS-1$
        }

        if (name.equals("MANAGE_NOTE")) { //$NON-NLS-1$
            return "manageNote"; //$NON-NLS-1$
        }

        if (name.equals("POLICY_EXEMPT")) { //$NON-NLS-1$
            return "policyExempt"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        if (name.equals("BRANCH_LEVEL_PERMISSIONS")) { //$NON-NLS-1$
            return "branchLevelPermissions"; //$NON-NLS-1$
        }

        return null;
    }
}
