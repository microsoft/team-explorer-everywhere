// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.ui.ISaveableFilter;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.Saveable;

import com.microsoft.tfs.util.Check;

/**
 * Adapts a {@link WorkbenchPartSaveableFilter} into an Eclipse
 * {@link ISaveableFilter}.
 *
 * @threadsafety unknown
 */
public class WorkbenchPartSaveableFilterAdapter implements ISaveableFilter {
    private final WorkbenchPartSaveableFilter filter;

    public WorkbenchPartSaveableFilterAdapter(final WorkbenchPartSaveableFilter filter) {
        Check.notNull(filter, "filter"); //$NON-NLS-1$

        this.filter = filter;
    }

    @Override
    public boolean select(final Saveable saveable, final IWorkbenchPart[] containingParts) {
        return filter.select(containingParts);
    }
}
