// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.ui.IWorkbenchPart;

/**
 * A simple saveable filter for editors. Similar to the Eclipse ISaveableFilter
 * interface, but does not take dependencies on Saveable. Used to adapt to
 * ISaveableFilter for backcompat.
 *
 * @threadsafety unknown
 */
public interface WorkbenchPartSaveableFilter {
    /**
     * Selects this item for saving.
     *
     * @param workbenchParts
     *        The workbench part(s) that have a dirty document.
     * @return <code>true</code> to save the document, <code>false</code> to not
     *         save.
     */
    boolean select(final IWorkbenchPart[] workbenchParts);
}
