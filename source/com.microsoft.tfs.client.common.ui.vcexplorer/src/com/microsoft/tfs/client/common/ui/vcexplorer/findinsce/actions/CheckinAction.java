// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckinTask;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditorInput;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class CheckinAction extends Action {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private FindInSourceControlEditor editor;

    public CheckinAction() {
        setImageDescriptor(imageHelper.getImageDescriptor("images/vc/checkin.gif")); //$NON-NLS-1$
        setToolTipText(Messages.getString("CheckinAction.ToolTipText")); //$NON-NLS-1$
    }

    public CheckinAction(final FindInSourceControlEditor editor) {
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
        if (editor == null || editor.getEditorInput() == null || !editor.getEditorInput().getQuery().showStatus()) {
            return false;
        }

        final Map<PendingChange, PendingSet> selectedPendingChanges = editor.getSelectedPendingChanges();

        return (selectedPendingChanges != null && !selectedPendingChanges.isEmpty() && isSelectedItemCheckedOut());
    }

    @Override
    public void run() {
        if (editor == null || editor.getEditorInput() == null) {
            return;
        }

        final FindInSourceControlEditorInput editorInput = editor.getEditorInput();
        final Map<PendingChange, PendingSet> selectedPendingChanges = editor.getSelectedPendingChanges();

        if (selectedPendingChanges != null && !selectedPendingChanges.isEmpty()) {
            final PendingChange[] pendingChanges = getPendingChanges(editorInput, selectedPendingChanges);

            new CheckinTask(editor.getSite().getShell(), editorInput.getRepository(), pendingChanges, null).run();
        }

        editor.run();
    }

    /**
     * filters and returns the list of pending changes checked out by the
     * current user in the current workspace from the given list
     *
     */
    private PendingChange[] getPendingChanges(
        final FindInSourceControlEditorInput editorInput,
        final Map<PendingChange, PendingSet> selectedPendingChanges) {
        final Workspace currentWorkspace = editor.getEditorInput().getRepository().getWorkspace();

        final ArrayList<PendingChange> pendingChanges = new ArrayList<PendingChange>();

        for (final Entry<PendingChange, PendingSet> entry : selectedPendingChanges.entrySet()) {
            final PendingChange pendingChange = entry.getKey();
            final PendingSet pendingSet = entry.getValue();

            if (Workspace.matchOwner(pendingSet.getOwnerName(), currentWorkspace.getOwnerName())
                && Workspace.matchComputer(pendingSet.getComputer(), currentWorkspace.getComputer())
                && Workspace.matchName(pendingSet.getName(), currentWorkspace.getName())) {
                pendingChanges.add(pendingChange);
            }
        }

        return pendingChanges.toArray(new PendingChange[pendingChanges.size()]);
    }

    /*
     * Checks if the selected item is currently checked out by the current user
     * in the current workspace
     */
    private boolean isSelectedItemCheckedOut() {
        final Map<PendingChange, PendingSet> selectedPendingChanges = editor.getSelectedPendingChanges();
        final Workspace currentWorkspace = editor.getEditorInput().getRepository().getWorkspace();

        if (selectedPendingChanges != null && !selectedPendingChanges.isEmpty()) {
            for (final PendingChange pendingChange : selectedPendingChanges.keySet()) {
                final PendingSet pendingSet = selectedPendingChanges.get(pendingChange);
                if (pendingSet.getName().equals(currentWorkspace.getName())
                    && pendingSet.getOwnerName().equals(currentWorkspace.getOwnerName())) {
                    return true;
                }
            }
        }

        return false;
    }

}
