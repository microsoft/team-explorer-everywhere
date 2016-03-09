// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

public class ApplicableRename implements Comparable<ApplicableRename> {
    private final String committedServerItem;
    private final RenameType renameType;
    private final RecursionType recursion;

    public ApplicableRename(
        final String committedServerItem,
        final RenameType renameType,
        final RecursionType recursionType) {
        this.committedServerItem = committedServerItem;
        this.renameType = renameType;
        this.recursion = recursionType;
    }

    @Override
    public int compareTo(final ApplicableRename other) {
        final int compare = ServerPath.compareTopDown(committedServerItem, other.committedServerItem);

        if (0 != compare) {
            return compare;
        }

        // Sort Additive after Subtractive.
        if (renameType == other.renameType) {
            return 0;
        } else if (renameType == RenameType.ADDITIVE) {
            return 1;
        } else {
            // other.RenameType == RenameType.Additive
            return -1;
        }
    }

    public String getCommittedServerItem() {
        return committedServerItem;
    }

    public RenameType getRenameType() {
        return renameType;
    }

    public RecursionType getRecursionType() {
        return recursion;
    }
}
