// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.util.BitField;

public class CommittedState extends BitField {
    private static final long serialVersionUID = 7719794811872968752L;

    public static final CommittedState COMMITTED = new CommittedState(1);
    public static final CommittedState UNCOMMITTED = new CommittedState(2);
    public static final CommittedState BOTH = new CommittedState(3);

    private CommittedState(final int flags) {
        super(flags);
    }

    public boolean contains(final CommittedState other) {
        return containsInternal(other);
    }
}
