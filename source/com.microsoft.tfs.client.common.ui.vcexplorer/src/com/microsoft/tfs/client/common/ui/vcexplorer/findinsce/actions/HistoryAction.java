// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewHistoryTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditorInput;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.StringUtil;

public class HistoryAction extends Action {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private FindInSourceControlEditor editor;

    public HistoryAction() {
        setImageDescriptor(imageHelper.getImageDescriptor("images/vc/history.gif")); //$NON-NLS-1$
        setToolTipText(Messages.getString("HistoryAction.ToolTipText")); //$NON-NLS-1$
    }

    public HistoryAction(final FindInSourceControlEditor editor) {
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

        final FindInSourceControlEditorInput editorInput = editor.getEditorInput();
        final TypedServerItem[] serverItems = editor.getSelectedServerItems();

        // Use local path if the item is mapped in the current workspace to
        // avoid problems with pending renames
        final String LocalPath =
            editorInput.getRepository().getWorkspace().getMappedLocalPath(serverItems[0].getServerPath());
        final String itemPath = !StringUtil.isNullOrEmpty(LocalPath) ? LocalPath : serverItems[0].getServerPath();

        if (serverItems != null && serverItems.length > 0) {
            new ViewHistoryTask(
                editor.getSite().getShell(),
                editorInput.getRepository(),
                new ItemSpec(
                    itemPath,
                    serverItems[0].getType() == ServerItemType.FILE ? RecursionType.NONE : RecursionType.FULL)).run();
        }
    }
}
