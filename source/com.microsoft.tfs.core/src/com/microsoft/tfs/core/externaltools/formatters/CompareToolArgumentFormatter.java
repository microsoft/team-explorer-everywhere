// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.formatters;

import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.util.Check;

/**
 * Takes individual string arguments for a compare tool and replaces the correct
 * placeholders ("%1", etc.) in the an {@link ExternalTool}'s arguments.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class CompareToolArgumentFormatter extends AbstractToolArgumentFormatter {
    public CompareToolArgumentFormatter() {
        super();
    }

    /**
     * Formats the compare arguments with the given substitutions.
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
     * @param originalLabel
     *        the label for the original file (must not be <code>null</code>)
     * @param modifiedLabel
     *        the label for the modified file (must not be <code>null</code>)
     * @return this tool's configured arguments after the given strings are
     *         substituted
     */
    public String[] formatArguments(
        final ExternalTool tool,
        final String original,
        final String modified,
        final String originalLabel,
        final String modifiedLabel) {
        Check.notNull(tool, "tool"); //$NON-NLS-1$
        Check.notNull(original, "original"); //$NON-NLS-1$
        Check.notNull(modified, "modified"); //$NON-NLS-1$
        Check.notNull(originalLabel, "originalLabel"); //$NON-NLS-1$
        Check.notNull(modifiedLabel, "modifiedLabel"); //$NON-NLS-1$

        return super.formatArguments(tool, new String[] {
            original,
            modified,
            "", //$NON-NLS-1$
            "", //$NON-NLS-1$
            "", //$NON-NLS-1$
            originalLabel,
            modifiedLabel,
            "", //$NON-NLS-1$
            "" //$NON-NLS-1$
        });
    }
}
