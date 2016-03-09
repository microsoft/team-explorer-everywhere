// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.util.BitField;

public class ConflictOptions extends BitField {
    private static final long serialVersionUID = -8023586407187705299L;

    public static final ConflictOptions NONE = new ConflictOptions(0);

    /**
     * Informs the client that it should disallow auto-merge.
     */
    public static final ConflictOptions DISALLOW_AUTO_MERGE = new ConflictOptions(1);

    /**
     * Indicates that this conflict is due to a local / pending version
     * mismatch.
     */
    public static final ConflictOptions PENDING_LOCAL_VERSION_MISMATCH = new ConflictOptions(2);

    /**
     * Current only used for version conflicts. This is set if the local pending
     * cahnges are redundant (cannot apply) to the version being moved to.
     */
    public static final ConflictOptions LOCAL_CHANGES_REDUNDANT_IN_TARGET_VERSION = new ConflictOptions(4);

    private ConflictOptions(final int flags) {
        super(flags);
    }

    public boolean contains(final ConflictOptions disallowAutoMerge) {
        return containsInternal(disallowAutoMerge);
    }

    public static ConflictOptions fromIntFlags(final int flags) {
        return new ConflictOptions(flags);
    }
}
