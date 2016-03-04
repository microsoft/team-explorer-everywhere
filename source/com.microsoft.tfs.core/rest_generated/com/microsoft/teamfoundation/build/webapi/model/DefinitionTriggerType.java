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
public enum DefinitionTriggerType {

    /**
    * Manual builds only.
    */
    NONE(1),
    /**
    * A build should be started for each changeset.
    */
    CONTINUOUS_INTEGRATION(2),
    /**
    * A build should be started for multiple changesets at a time at a specified interval.
    */
    BATCHED_CONTINUOUS_INTEGRATION(4),
    /**
    * A build should be started on a specified schedule whether or not changesets exist.
    */
    SCHEDULE(8),
    /**
    * A validation build should be started for each check-in.
    */
    GATED_CHECK_IN(16),
    /**
    * A validation build should be started for each batch of check-ins.
    */
    BATCHED_GATED_CHECK_IN(32),
    /**
    * All types.
    */
    ALL(63),
    ;

    private int value;

    private DefinitionTriggerType(final int value) {
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

        if (name.equals("CONTINUOUS_INTEGRATION")) { //$NON-NLS-1$
            return "continuousIntegration"; //$NON-NLS-1$
        }

        if (name.equals("BATCHED_CONTINUOUS_INTEGRATION")) { //$NON-NLS-1$
            return "batchedContinuousIntegration"; //$NON-NLS-1$
        }

        if (name.equals("SCHEDULE")) { //$NON-NLS-1$
            return "schedule"; //$NON-NLS-1$
        }

        if (name.equals("GATED_CHECK_IN")) { //$NON-NLS-1$
            return "gatedCheckIn"; //$NON-NLS-1$
        }

        if (name.equals("BATCHED_GATED_CHECK_IN")) { //$NON-NLS-1$
            return "batchedGatedCheckIn"; //$NON-NLS-1$
        }

        if (name.equals("ALL")) { //$NON-NLS-1$
            return "all"; //$NON-NLS-1$
        }

        return null;
    }
}
