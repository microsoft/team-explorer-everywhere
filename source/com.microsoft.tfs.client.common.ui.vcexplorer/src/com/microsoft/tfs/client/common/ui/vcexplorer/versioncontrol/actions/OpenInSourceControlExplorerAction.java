// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.TFSVersionControlExplorerPlugin;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditorInput;

public class OpenInSourceControlExplorerAction extends Action {
    private final ImageHelper imageHelper = new ImageHelper(TFSVersionControlExplorerPlugin.PLUGIN_ID);

    private FindInSourceControlEditor editor;

    public OpenInSourceControlExplorerAction() {
        setImageDescriptor(imageHelper.getImageDescriptor("icons/VersionControl.gif")); //$NON-NLS-1$
        setToolTipText(Messages.getString("OpenInSourceControlExplorerAction.ToolTipText")); //$NON-NLS-1$
    }

    public OpenInSourceControlExplorerAction(final FindInSourceControlEditor editor) {
        this();

        setActiveEditor(editor);
    }

    public void setActiveEditor(final FindInSourceControlEditor editor) {
        this.editor = editor;
        setEnabled(isEnabled());

        this.editor.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                setEnabled(isEnabled());
                editor.getEditorSite().getActionBars().getToolBarManager().update(true);
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return (editor != null && editor.getEditorInput() != null && editor.isSingleItemSelected());
    }

    @Override
    public void run() {
        if (editor == null || editor.getEditorInput() == null) {
            return;
        }

        final TypedServerItem[] serverItems = editor.getSelectedServerItems();
        if (serverItems == null || serverItems.length == 0) {
            return;
        }

        // Open the Source Control Explorer.
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        final IWorkbenchPage page = window.getActivePage();
        try {
            page.openEditor(new VersionControlEditorInput(), VersionControlEditor.ID);
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }

        window.getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                selectFileOrFolder(serverItems[0].getType(), serverItems[0].getServerPath());
            }
        });
    }

    private void selectFileOrFolder(final ServerItemType type, final String serverPath) {
        final VersionControlEditor editor = VersionControlEditor.getCurrent();
        if (editor != null) {
            if (ServerItemType.isFile(type)) {
                editor.setSelectedFile(new ServerItemPath(serverPath));
            } else {
                editor.setSelectedFolder(new ServerItemPath(serverPath));
            }
        }
    }
}
