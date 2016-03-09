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
public enum GitRefUpdateStatus {

    /**
    * Indicates that the ref update request was completed successfully.
    */
    SUCCEEDED(0),
    /**
    * Indicates that the ref update request could not be completed because part of the graph would be disconnected by this change, and the caller does not have ForcePush permission on the repository.
    */
    FORCE_PUSH_REQUIRED(1),
    /**
    * Indicates that the ref update request could not be completed because the old object ID presented in the request was not the object ID of the ref when the database attempted the update. The most likely scenario is that the caller lost a race to update the ref.
    */
    STALE_OLD_OBJECT_ID(2),
    /**
    * Indicates that the ref update request could not be completed because the ref name presented in the request was not valid.
    */
    INVALID_REF_NAME(3),
    /**
    * The request was not processed
    */
    UNPROCESSED(4),
    /**
    * The ref update request could not be completed because the new object ID for the ref could not be resolved to a commit object (potentially through any number of tags)
    */
    UNRESOLVABLE_TO_COMMIT(5),
    /**
    * The ref update request could not be completed because the user lacks write permissions required to write this ref
    */
    WRITE_PERMISSION_REQUIRED(6),
    /**
    * The ref update request could not be completed because the user lacks note creation permissions required to write this note
    */
    MANAGE_NOTE_PERMISSION_REQUIRED(7),
    /**
    * The ref update request could not be completed because the user lacks the permission to create a branch
    */
    CREATE_BRANCH_PERMISSION_REQUIRED(8),
    /**
    * The ref update request could not be completed because the user lacks the permission to create a tag
    */
    CREATE_TAG_PERMISSION_REQUIRED(9),
    /**
    * The ref update could not be completed because it was rejected by the plugin.
    */
    REJECTED_BY_PLUGIN(10),
    /**
    * The ref update could not be completed because the ref is locked by another user.
    */
    LOCKED(11),
    /**
    * The ref update could not be completed because, in case-insensitive mode, the ref name conflicts with an existing, differently-cased ref name.
    */
    REF_NAME_CONFLICT(12),
    /**
    * The ref update could not be completed because it was rejected by policy.
    */
    REJECTED_BY_POLICY(13),
    /**
    * Indicates that the ref update request was completed successfully, but the ref doesn't actually exist so no changes were made.  This should only happen during deletes.
    */
    SUCCEEDED_NON_EXISTENT_REF(14),
    /**
    * Indicates that the ref update request was completed successfully, but the passed-in ref was corrupt - as in, the old object ID was bad.  This should only happen during deletes.
    */
    SUCCEEDED_CORRUPT_REF(15),
    ;

    private int value;

    private GitRefUpdateStatus(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("SUCCEEDED")) { //$NON-NLS-1$
            return "succeeded"; //$NON-NLS-1$
        }

        if (name.equals("FORCE_PUSH_REQUIRED")) { //$NON-NLS-1$
            return "forcePushRequired"; //$NON-NLS-1$
        }

        if (name.equals("STALE_OLD_OBJECT_ID")) { //$NON-NLS-1$
            return "staleOldObjectId"; //$NON-NLS-1$
        }

        if (name.equals("INVALID_REF_NAME")) { //$NON-NLS-1$
            return "invalidRefName"; //$NON-NLS-1$
        }

        if (name.equals("UNPROCESSED")) { //$NON-NLS-1$
            return "unprocessed"; //$NON-NLS-1$
        }

        if (name.equals("UNRESOLVABLE_TO_COMMIT")) { //$NON-NLS-1$
            return "unresolvableToCommit"; //$NON-NLS-1$
        }

        if (name.equals("WRITE_PERMISSION_REQUIRED")) { //$NON-NLS-1$
            return "writePermissionRequired"; //$NON-NLS-1$
        }

        if (name.equals("MANAGE_NOTE_PERMISSION_REQUIRED")) { //$NON-NLS-1$
            return "manageNotePermissionRequired"; //$NON-NLS-1$
        }

        if (name.equals("CREATE_BRANCH_PERMISSION_REQUIRED")) { //$NON-NLS-1$
            return "createBranchPermissionRequired"; //$NON-NLS-1$
        }

        if (name.equals("CREATE_TAG_PERMISSION_REQUIRED")) { //$NON-NLS-1$
            return "createTagPermissionRequired"; //$NON-NLS-1$
        }

        if (name.equals("REJECTED_BY_PLUGIN")) { //$NON-NLS-1$
            return "rejectedByPlugin"; //$NON-NLS-1$
        }

        if (name.equals("LOCKED")) { //$NON-NLS-1$
            return "locked"; //$NON-NLS-1$
        }

        if (name.equals("REF_NAME_CONFLICT")) { //$NON-NLS-1$
            return "refNameConflict"; //$NON-NLS-1$
        }

        if (name.equals("REJECTED_BY_POLICY")) { //$NON-NLS-1$
            return "rejectedByPolicy"; //$NON-NLS-1$
        }

        if (name.equals("SUCCEEDED_NON_EXISTENT_REF")) { //$NON-NLS-1$
            return "succeededNonExistentRef"; //$NON-NLS-1$
        }

        if (name.equals("SUCCEEDED_CORRUPT_REF")) { //$NON-NLS-1$
            return "succeededCorruptRef"; //$NON-NLS-1$
        }

        return null;
    }
}
