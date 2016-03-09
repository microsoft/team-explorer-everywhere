// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.ui.framework.action.ObjectActionDelegate;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.product.ProductInformation;

public abstract class BaseAction extends ObjectActionDelegate implements IEditorActionDelegate {
    private IEditorPart editor;
    private IBuildServer buildServer;

    /**
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
     *      org.eclipse.ui.IEditorPart)
     */
    @Override
    public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
        super.setActivePart(action, targetEditor);
        editor = targetEditor;
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.framework.action.ObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
     *      org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
        super.setActivePart(action, targetPart);
        if (targetPart instanceof IEditorPart) {
            setActiveEditor(action, (IEditorPart) targetPart);
        }
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.framework.action.ObjectActionDelegate#onSelectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (selection == null) {
            return;
        }

        final Object obj = getSelectionFirstElement();
        if (obj instanceof IQueuedBuild) {
            buildServer = ((IQueuedBuild) obj).getBuildServer();
        } else if (obj instanceof IBuildDetail) {
            buildServer = ((IBuildDetail) obj).getBuildServer();
        }
    }

    public void fireRefresh() {
        MessageDialog.openError(
            getShell(),
            ProductInformation.getCurrent().getFamilyShortName(),
            "BaseAction.fireRefresh() needs impl."); //$NON-NLS-1$
        // UiPlugin.getDefault().fireEvent(new RefreshEvent());
    }

    protected void openErrorDialog(final String errorMessage) {
        MessageDialog.openError(getShell(), ProductInformation.getCurrent().getFamilyShortName(), errorMessage);
    }

    protected void openInformationDialog(final String infoMessage) {
        MessageDialog.openInformation(getShell(), ProductInformation.getCurrent().getFamilyShortName(), infoMessage);
    }

    /**
     * @return the editor
     */
    public IEditorPart getEditor() {
        return editor;
    }

    /**
     * @return the buildServer
     */
    public IBuildServer getBuildServer() {
        return buildServer;
    }

}
