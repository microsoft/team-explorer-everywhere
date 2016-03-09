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
public enum BuildReason {

    /**
    * No reason. This value should not be used.
    */
    NONE(0),
    /**
    * The build was started manually.
    */
    MANUAL(1),
    /**
    * The build was started for the trigger TriggerType.ContinuousIntegration.
    */
    INDIVIDUAL_C_I(2),
    /**
    * The build was started for the trigger TriggerType.BatchedContinuousIntegration.
    */
    BATCHED_C_I(4),
    /**
    * The build was started for the trigger TriggerType.Schedule.
    */
    SCHEDULE(8),
    /**
    * The build was created by a user.
    */
    USER_CREATED(32),
    /**
    * The build was started manually for private validation.
    */
    VALIDATE_SHELVESET(64),
    /**
    * The build was started for the trigger ContinuousIntegrationType.Gated.
    */
    CHECK_IN_SHELVESET(128),
    /**
    * The build was triggered for retention policy purposes.
    */
    TRIGGERED(175),
    /**
    * All reasons.
    */
    ALL(239),
    ;

    private int value;

    private BuildReason(final int value) {
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

        if (name.equals("MANUAL")) { //$NON-NLS-1$
            return "manual"; //$NON-NLS-1$
        }

        if (name.equals("INDIVIDUAL_C_I")) { //$NON-NLS-1$
            return "individualCI"; //$NON-NLS-1$
        }

        if (name.equals("BATCHED_C_I")) { //$NON-NLS-1$
            return "batchedCI"; //$NON-NLS-1$
        }

        if (name.equals("SCHEDULE")) { //$NON-NLS-1$
            return "schedule"; //$NON-NLS-1$
        }

        if (name.equals("USER_CREATED")) { //$NON-NLS-1$
            return "userCreated"; //$NON-NLS-1$
        }

        if (name.equals("VALIDATE_SHELVESET")) { //$NON-NLS-1$
            return "validateShelveset"; //$NON-NLS-1$
        }

        if (name.equals("CHECK_IN_SHELVESET")) { //$NON-NLS-1$
            return "checkInShelveset"; //$NON-NLS-1$
        }

        if (name.equals("TRIGGERED")) { //$NON-NLS-1$
            return "triggered"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        return null;
    }
}
