// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import com.microsoft.tfs.util.BitField;

/**
 * @since TEE-SDK-10.1
 */
public class ConflictResolutionOptions extends BitField {
    public static final ConflictResolutionOptions NONE = new ConflictResolutionOptions(0);
    public static final ConflictResolutionOptions SELECT_NAME = new ConflictResolutionOptions(1);
    public static final ConflictResolutionOptions SELECT_ENCODING = new ConflictResolutionOptions(2);

    private ConflictResolutionOptions(final int flags) {
        super(flags);
    }

    public boolean contains(final ConflictResolutionOptions other) {
        return containsInternal(other);
    }

    public ConflictResolutionOptions combine(final ConflictResolutionOptions other) {
        return new ConflictResolutionOptions(combineInternal(other));
    }
}
