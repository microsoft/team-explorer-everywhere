// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

public class WorkItemEditorActionContributor extends EditorActionBarContributor {
    // private IAction myAction;

    @Override
    public void contributeToToolBar(final IToolBarManager toolBarManager) {
        // myAction = new MyAction();
        // toolBarManager.add(myAction);
    }

    @Override
    public void setActiveEditor(final IEditorPart targetEditor) {
        // if (myAction != null)
        // {
        // myAction.setActiveEditor((WITEditor) targetEditor);
        // }
    }

}
