// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.CheckinAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.CheckoutAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.CopyToClipboardAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.HistoryAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.ModifyQueryAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.PropertiesAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.RefreshAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.UndoAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.OpenInSourceControlExplorerAction;

public class FindInSourceControlEditorActionContributor extends EditorActionBarContributor {
    private FindInSourceControlEditor activeEditor;

    private RefreshAction refreshAction;
    private CheckoutAction checkoutAction;
    private CheckinAction checkinAction;
    private CopyToClipboardAction copyAction;
    private UndoAction undoAction;
    private HistoryAction historyAction;
    private OpenInSourceControlExplorerAction openInSCEAction;
    private PropertiesAction propertiesAction;
    private ModifyQueryAction modifyQueryAction;

    @Override
    public void contributeToToolBar(final IToolBarManager toolBarManager) {
        refreshAction = new RefreshAction();
        checkoutAction = new CheckoutAction();
        checkinAction = new CheckinAction();
        copyAction = new CopyToClipboardAction();
        undoAction = new UndoAction();
        historyAction = new HistoryAction();
        openInSCEAction = new OpenInSourceControlExplorerAction();
        propertiesAction = new PropertiesAction();
        modifyQueryAction = new ModifyQueryAction();

        toolBarManager.add(refreshAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(checkoutAction);
        toolBarManager.add(checkinAction);
        toolBarManager.add(undoAction);
        toolBarManager.add(historyAction);
        toolBarManager.add(openInSCEAction);
        toolBarManager.add(propertiesAction);
        toolBarManager.add(copyAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(modifyQueryAction);
    }

    @Override
    public void setActiveEditor(final IEditorPart targetEditor) {
        // Get the new active editor and setup a listener.
        activeEditor = (FindInSourceControlEditor) targetEditor;

        if (refreshAction != null) {
            refreshAction.setActiveEditor(activeEditor);
        }

        if (checkoutAction != null) {
            checkoutAction.setActiveEditor(activeEditor);
        }

        if (checkinAction != null) {
            checkinAction.setActiveEditor(activeEditor);
        }

        if (undoAction != null) {
            undoAction.setActiveEditor(activeEditor);
        }

        if (historyAction != null) {
            historyAction.setActiveEditor(activeEditor);
        }

        if (openInSCEAction != null) {
            openInSCEAction.setActiveEditor(activeEditor);
        }

        if (propertiesAction != null) {
            propertiesAction.setActiveEditor(activeEditor);
        }
        if (copyAction != null) {
            copyAction.setActiveEditor(activeEditor);
        }
        if (modifyQueryAction != null) {
            modifyQueryAction.setActiveEditor(activeEditor);
        }

        getActionBars().getToolBarManager().update(true);
    }
}
