// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.serveritem;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class TypedServerGitItem extends TypedServerItem {
    public TypedServerGitItem(final String serverPath, final ServerItemType type) {
        super(serverPath, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TypedServerItem)) {
            return false;
        }

        final TypedServerItem other = (TypedServerItem) obj;

        return haveSimilarTypes(getType(), other.getType())
            && ServerPath.equals(getServerPath(), other.getServerPath());
    }

    private boolean haveSimilarTypes(final ServerItemType thisType, final ServerItemType otherType) {
        if (thisType == otherType) {
            return true;
        }

        if (otherType == ServerItemType.FOLDER) {
            return thisType == ServerItemType.GIT_REPOSITORY || thisType == ServerItemType.GIT_BRANCH;
        }

        if (thisType == ServerItemType.FOLDER) {
            return otherType == ServerItemType.GIT_REPOSITORY || otherType == ServerItemType.GIT_BRANCH;
        }

        return false;
    }
}
