// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.util.Set;
import java.util.TreeSet;

import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

public class PathWatcherReport {
    private boolean fullyInvalidated;
    private final Set<String> changedPaths;

    private static int MAX_CHANGED_PATHS = 128;

    public PathWatcherReport(final boolean initiallyInvalidated) {
        this.fullyInvalidated = initiallyInvalidated;
        this.changedPaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    }

    public boolean addChangedPath(final String path) {
        // Only return false from this method when m_changedPaths already
        // contained the path. If we're in the fully invalidated state, return
        // true.
        boolean added = true;

        if (!fullyInvalidated) {
            added = changedPaths.add(path);

            if (added && changedPaths.size() > MAX_CHANGED_PATHS) {
                // We're now over the cap.
                fullyInvalidate();
            }
        }

        // If a .tfignore file was changed, then we always fully invalidate the
        // scanner.
        if (LocaleInvariantStringHelpers.caseInsensitiveEndsWith(
            path,
            LocalItemExclusionEvaluator.IGNORE_FILE_NAME_WITH_SEPARATOR_PREFIX)) {
            added = true;
            fullyInvalidate();
        }

        return added;
    }

    public void fullyInvalidate() {
        fullyInvalidated = true;
        changedPaths.clear();
    }

    public void unionWith(final PathWatcherReport otherReport) {
        if (!fullyInvalidated) {
            if (otherReport.fullyInvalidated) {
                fullyInvalidate();
            } else {
                for (final String path : otherReport.changedPaths) {
                    addChangedPath(path);

                    if (fullyInvalidated) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * True if the values in ChangedPaths should be ignored, because a full
     * invalidation has occurred.
     */
    public boolean getFullyInvalidated() {
        return fullyInvalidated;
    }

    /**
     * Returns true if FullyInvalidated is false, and the ChangedPaths list is
     * also empty.
     */
    public boolean isNothingChanged() {
        return !fullyInvalidated && changedPaths.size() == 0;
    }

    /**
     * If FullyInvalidated is false, then this contains the list of paths which
     * have changed since the last time this PathWatcher was polled.
     */
    public Iterable<String> getChangedPaths() {
        return changedPaths;
    }
}
