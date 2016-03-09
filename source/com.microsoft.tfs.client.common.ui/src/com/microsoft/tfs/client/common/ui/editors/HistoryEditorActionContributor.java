// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.editors;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

/**
 *
 * @threadsafety unknown
 */
public class HistoryEditorActionContributor extends EditorActionBarContributor {
    @Override
    public void contributeToToolBar(final IToolBarManager toolBarManager) {
    }

    @Override
    public void setActiveEditor(final IEditorPart targetEditor) {
        getActionBars().getToolBarManager().update(true);
    }
}
