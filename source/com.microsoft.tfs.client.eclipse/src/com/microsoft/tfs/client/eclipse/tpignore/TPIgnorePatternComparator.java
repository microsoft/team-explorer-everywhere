// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.tpignore;

import java.util.Comparator;
import java.util.regex.Pattern;

import com.microsoft.tfs.util.Check;

public class TPIgnorePatternComparator implements Comparator<Pattern> {
    public TPIgnorePatternComparator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final Pattern pattern1, final Pattern pattern2) {
        Check.notNull(pattern1, "pattern1"); //$NON-NLS-1$
        Check.notNull(pattern2, "pattern2"); //$NON-NLS-1$

        return pattern1.pattern().compareTo(pattern2.pattern());
    }
}