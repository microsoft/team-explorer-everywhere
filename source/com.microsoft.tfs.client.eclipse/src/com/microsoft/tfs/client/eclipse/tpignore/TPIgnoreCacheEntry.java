// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.tpignore;

import java.util.regex.Pattern;

/**
 * An entry for use in an <code>IgnoreDetectedResourcesCache</code>. Contains
 * the date these patterns were last loaded from disk and the patterns.
 */
final class TPIgnoreCacheEntry {
    private final long loadedFromDiskTime;
    private final Pattern[] patterns;

    public TPIgnoreCacheEntry(final long loadedFromDiskTime, final Pattern[] patterns) {
        this.loadedFromDiskTime = loadedFromDiskTime;
        this.patterns = patterns;
    }

    public long getLoadedFromDiskTime() {
        return loadedFromDiskTime;
    }

    public Pattern[] getPatterns() {
        return patterns;
    }
}
