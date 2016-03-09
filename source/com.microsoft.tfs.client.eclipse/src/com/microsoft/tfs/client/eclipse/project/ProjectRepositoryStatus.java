// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import com.microsoft.tfs.util.BitField;

public class ProjectRepositoryStatus extends BitField {
    private static final long serialVersionUID = 3612900243909425542L;

    /*
     * Used internally by ProjectRepositoryManager, only to begin initializing
     * connections.
     */
    public final static ProjectRepositoryStatus INITIALIZING = new ProjectRepositoryStatus(1);

    /* Valid repository states */
    public final static ProjectRepositoryStatus CONNECTING = new ProjectRepositoryStatus(2);
    public final static ProjectRepositoryStatus ONLINE = new ProjectRepositoryStatus(4);
    public final static ProjectRepositoryStatus OFFLINE = new ProjectRepositoryStatus(8);

    private ProjectRepositoryStatus(final int value) {
        super(value);
    }

    public ProjectRepositoryStatus combine(final ProjectRepositoryStatus other) {
        return new ProjectRepositoryStatus(combineInternal(other));
    }

    public boolean contains(final ProjectRepositoryStatus other) {
        return containsInternal(other);
    }

    public int getValue() {
        return toIntFlags();
    }
}
