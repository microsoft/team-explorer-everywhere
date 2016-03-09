// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;

/**
 * Used by {@link LocalItemExclusionEvaluator} and {@link IgnoreFile}.
 *
 * @threadsafety immutable
 */
class IgnoreEntry {
    // The original exclusion, verbatim
    public final String originalExclusion;

    // The path and pattern for evaluation
    public final String path;
    public final String pattern;

    // True if this is an exclusion, false if it is an inclusion
    public final boolean isExcluded;

    // True if this pattern is applied recursively, false otherwise
    public final boolean isRecursive;

    // True if this pattern should only apply to folders, false otherwise
    public final boolean isFolderOnly;

    // Describes how to match Pattern
    public final boolean isEndsWith;
    public final boolean isStartsWith;
    public final boolean isComplex;

    /**
     * For .tfignore entries.
     */
    public IgnoreEntry(
        final String fullPath,
        final boolean isExcluded,
        final boolean isRecursive,
        final boolean isFolderOnly) {
        this.originalExclusion = fullPath;

        final String path = LocalPath.getParent(fullPath);
        final String pattern = LocalPath.getFileName(fullPath);

        this.path = path;

        final AtomicBoolean isEndsWith = new AtomicBoolean();
        final AtomicBoolean isStartsWith = new AtomicBoolean();
        final AtomicBoolean isComplex = new AtomicBoolean();
        this.pattern = acceptPattern(pattern, isEndsWith, isStartsWith, isComplex);
        this.isEndsWith = isEndsWith.get();
        this.isStartsWith = isStartsWith.get();
        this.isComplex = isComplex.get();

        this.isExcluded = isExcluded;
        this.isRecursive = isRecursive;
        this.isFolderOnly = isFolderOnly;
    }

    /**
     * For global exclusion list entries.
     */
    public IgnoreEntry(final String pattern) {
        this.originalExclusion = pattern;

        this.path = null;

        final AtomicBoolean isEndsWith = new AtomicBoolean();
        final AtomicBoolean isStartsWith = new AtomicBoolean();
        final AtomicBoolean isComplex = new AtomicBoolean();
        this.pattern = acceptPattern(pattern, isEndsWith, isStartsWith, isComplex);
        this.isEndsWith = isEndsWith.get();
        this.isStartsWith = isStartsWith.get();
        this.isComplex = isComplex.get();

        this.isExcluded = true;
        this.isRecursive = true;
        this.isFolderOnly = false;
    }

    /**
     * Helper method for the constructor.
     */
    private String acceptPattern(
        String pattern,
        final AtomicBoolean isEndsWith,
        final AtomicBoolean isStartsWith,
        final AtomicBoolean isComplex) {
        isEndsWith.set(false);
        isStartsWith.set(false);
        isComplex.set(false);

        if (Wildcard.isWildcard(pattern)) {
            if (pattern.length() >= 2
                && pattern.charAt(0) == '*'
                && !Wildcard.isWildcard(pattern.substring(1, pattern.length()))) {
                pattern = pattern.substring(1);
                isEndsWith.set(true);
            } else if (pattern.length() >= 2
                && pattern.charAt(pattern.length() - 1) == '*'
                && !Wildcard.isWildcard(pattern.substring(0, pattern.length() - 1))) {
                pattern = pattern.substring(0, pattern.length() - 1);
                isStartsWith.set(true);
            } else {
                isComplex.set(true);
            }
        }

        return pattern;
    }
}
