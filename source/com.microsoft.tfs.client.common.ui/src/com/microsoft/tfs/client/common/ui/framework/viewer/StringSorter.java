// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import java.text.Collator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Simple sorter for table and tree views that does a string comparison of the
 * objects toString methods.
 */
public class StringSorter extends ViewerSorter {

    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
        final Collator collator = Collator.getInstance();

        return collator.compare(e1.toString(), e2.toString());
    }
}
