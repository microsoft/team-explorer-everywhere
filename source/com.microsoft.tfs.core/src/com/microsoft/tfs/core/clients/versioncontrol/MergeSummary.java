// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

/**
 * Describes the results of a merge performed by the three-way merge
 * implementation.
 *
 * @since TEE-SDK-10.1
 */
public final class MergeSummary {
    private final int totalCommonLines;
    private final int totalLocalChangedLines;
    private final int totalLatestChangedLines;
    private final int totalCommonChangedLines;
    private final int totalConflictingLines;

    public MergeSummary(
        final int totalCommonLines,
        final int totalLocalChangedLines,
        final int totalLatestChangedLines,
        final int totalCommonChangedLines,
        final int totalConflictingLines) {
        this.totalCommonLines = totalCommonLines;
        this.totalLocalChangedLines = totalLocalChangedLines;
        this.totalLatestChangedLines = totalLatestChangedLines;
        this.totalCommonChangedLines = totalCommonChangedLines;
        this.totalConflictingLines = totalConflictingLines;
    }

    /**
     * @return the total number of lines in common in the merge
     */
    public int getTotalCommonLines() {
        return totalCommonLines;
    }

    /**
     * @return the total number of lines changed between the common file and the
     *         local (workspace) file
     */
    public int getLocalChangedLines() {
        return totalLocalChangedLines;
    }

    /**
     * @return the total number of lines changed between the common file and the
     *         latest (server) file
     */
    public int getLatestChangedLines() {
        return totalLatestChangedLines;
    }

    /**
     * @return the total number of lines that were changed to the same value in
     *         both the local (workspace) and latest (server) files
     */
    public int getCommonChangedLines() {
        return totalCommonChangedLines;
    }

    /**
     * @return the total number of conflicting lines in the merge (lines that
     *         changed in both the local and latest files)
     */
    public int getTotalConflictingLines() {
        return totalConflictingLines;
    }
}
