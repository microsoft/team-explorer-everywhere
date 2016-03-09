// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.tpignore;

import com.microsoft.tfs.util.Check;

/**
 * One line in a {@link TPIgnoreDocument}.
 *
 * @threadsafety immutable
 */
public class Line {
    private final String contents;

    /**
     * Constructs a {@link Line} with the specified contents. Do not include
     * newlines.
     *
     * @param contents
     *        the line's contents (do not include newlines) (must not be
     *        <code>null</code>)
     */
    public Line(final String contents) {
        Check.notNull(contents, "contents"); //$NON-NLS-1$
        this.contents = contents;
    }

    /**
     * @return the line's contents (does not include newlines)
     */
    public String getContents() {
        return contents;
    }

    @Override
    public String toString() {
        return getContents();
    }
}