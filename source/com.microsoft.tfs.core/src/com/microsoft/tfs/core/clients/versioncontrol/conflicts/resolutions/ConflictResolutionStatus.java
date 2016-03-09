// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * A typed enum representing the status of a running conflict resolution.
 *
 * @since TEE-SDK-10.1
 */
public class ConflictResolutionStatus extends TypesafeEnum {
    public static final ConflictResolutionStatus NOT_STARTED = new ConflictResolutionStatus(0);
    public static final ConflictResolutionStatus RUNNING = new ConflictResolutionStatus(1);
    public static final ConflictResolutionStatus FAILED = new ConflictResolutionStatus(2);
    public static final ConflictResolutionStatus SUCCESS = new ConflictResolutionStatus(3);
    public static final ConflictResolutionStatus CANCELLED = new ConflictResolutionStatus(4);
    public static final ConflictResolutionStatus SUCCEEDED_WITH_CONFLICTS = new ConflictResolutionStatus(5);

    protected ConflictResolutionStatus(final int status) {
        super(status);
    }

    public boolean isCompletionStatus() {
        if (this == FAILED || this == CANCELLED || this == SUCCESS || this == SUCCEEDED_WITH_CONFLICTS) {
            return true;
        }

        return false;
    }
}
