// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class ServerItemIsCommittedTuple {
    private final String committedServerItem;
    private final boolean isCommitted;

    public ServerItemIsCommittedTuple(final String committedServerItem, final boolean isCommitted) {
        this.committedServerItem = committedServerItem;
        this.isCommitted = isCommitted;
    }

    public String getCommittedServerItem() {
        return committedServerItem;
    }

    public boolean isCommitted() {
        return isCommitted;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof ServerItemIsCommittedTuple == false) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        final ServerItemIsCommittedTuple other = (ServerItemIsCommittedTuple) obj;
        return isCommitted == other.isCommitted && ServerPath.equals(committedServerItem, other.committedServerItem);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + ((committedServerItem == null) ? 0 : ServerPath.hashCode(committedServerItem));
        result = result * 37 + ((isCommitted) ? 1 : 0);

        return result;
    }
}
