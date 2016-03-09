// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.item;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;

/**
 * Represents a local filesystem item's path on disk. Knows the hosts filesystem
 * preference for case sensitivity.
 *
 * @threadsafety thread safe
 */
public final class LocalItemPath {
    private final String path;

    public LocalItemPath(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        this.path = LocalPath.canonicalize(path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !(obj instanceof LocalItemPath)) {
            return false;
        }

        final LocalItemPath other = (LocalItemPath) obj;
        return LocalPath.equals(path, other.getPath());
    }

    @Override
    public int hashCode() {
        return LocalPath.hashCode(path);
    }
}
