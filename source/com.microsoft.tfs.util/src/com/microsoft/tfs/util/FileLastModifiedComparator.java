// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.File;
import java.util.Comparator;

/**
 * Compares java.io.File objects based on their lastModified property.
 */
public class FileLastModifiedComparator implements Comparator {
    boolean ascending = true;

    /**
     * Sets whether this comparator sorts in an ascending order. The ascending
     * order is defined as earlier modification dates will sort before later
     * modification dates. The default is ascending = true.
     *
     * @param ascending
     *        sort order
     */
    public void setAscending(final boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(final Object arg0, final Object arg1) {
        final File f1 = (File) arg0;
        final File f2 = (File) arg1;
        if (f1.lastModified() > f2.lastModified()) {
            return (ascending ? 1 : -1);
        } else if (f1.lastModified() < f2.lastModified()) {
            return (ascending ? -1 : 1);
        }
        return 0;
    }
}
