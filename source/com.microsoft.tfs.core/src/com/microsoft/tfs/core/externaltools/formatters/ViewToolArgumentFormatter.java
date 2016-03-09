// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.formatters;

import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.util.Check;

/**
 * Takes individual string arguments for a view tool and replaces the correct
 * placeholders ("%1", etc.) in the an {@link ExternalTool}'s arguments.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class ViewToolArgumentFormatter extends AbstractToolArgumentFormatter {
    public ViewToolArgumentFormatter() {
        super();
    }

    /**
     * Formats the view arguments with the given substitutions.
     *
     * @param tool
     *        the {@link ExternalTool} to format arguments for (must not be
     *        <code>null</code>)
     * @param filename
     *        the file to view (must not be <code>null</code>)
     * @return this tool's configured arguments after the given strings are
     *         substituted
     */
    public String[] formatArguments(final ExternalTool tool, final String filename) {
        Check.notNull(tool, "tool"); //$NON-NLS-1$
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        return super.formatArguments(tool, new String[] {
            filename
        });
    }

}
