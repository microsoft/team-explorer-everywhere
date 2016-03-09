// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.formatters;

import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.util.Check;

/**
 * Takes individual string arguments for a merge tool and replaces the correct
 * placeholders ("%1", etc.) in the an {@link ExternalTool}'s arguments.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class MergeToolArgumentFormatter extends AbstractToolArgumentFormatter {
    public MergeToolArgumentFormatter() {
        super();
    }

    /**
     * Formats the merge arguments with the given substitutions.
     *
     * @param tool
     *        the {@link ExternalTool} to format arguments for (must not be
     *        <code>null</code>)
     * @param original
     *        the filename of the "original" file (server version) (must not be
     *        <code>null</code>)
     * @param modified
     *        the filename of the "modified" file (local version) (must not be
     *        <code>null</code>)
     * @param base
     *        the filename of the "base" file (common ancestor) (must not be
     *        <code>null</code>)
     * @param merged
     *        the filename of the "merged" file (output by merger) (must not be
     *        <code>null</code>)
     * @param originalLabel
     *        the label for the original file formatArguments
     * @param modifiedLabel
     *        the label for the modified file formatArguments
     * @param baseLabel
     *        the label for the base file formatArguments
     * @param mergedLabel
     *        the label for the merged file formatArguments
     * @return this tool's configured arguments after the given strings are
     *         substituted
     */
    public String[] formatArguments(
        final ExternalTool tool,
        final String original,
        final String modified,
        final String base,
        final String merged,
        final String originalLabel,
        final String modifiedLabel,
        final String baseLabel,
        final String mergedLabel) {
        Check.notNull(tool, "tool"); //$NON-NLS-1$
        Check.notNull(original, "original"); //$NON-NLS-1$
        Check.notNull(modified, "modified"); //$NON-NLS-1$
        Check.notNull(base, "base"); //$NON-NLS-1$
        Check.notNull(merged, "merged"); //$NON-NLS-1$
        Check.notNull(originalLabel, "originalLabel"); //$NON-NLS-1$
        Check.notNull(modifiedLabel, "modifiedLabel"); //$NON-NLS-1$
        Check.notNull(baseLabel, "baseLabel"); //$NON-NLS-1$
        Check.notNull(mergedLabel, "mergedLabel"); //$NON-NLS-1$

        return super.formatArguments(tool, new String[] {
            original,
            modified,
            base,
            merged,
            "", //$NON-NLS-1$
            originalLabel,
            modifiedLabel,
            baseLabel,
            mergedLabel
        });
    }
}
