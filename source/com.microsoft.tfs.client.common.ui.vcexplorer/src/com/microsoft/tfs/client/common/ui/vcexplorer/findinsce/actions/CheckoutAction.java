// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckoutWithPromptTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditorInput;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class CheckoutAction extends Action {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private FindInSourceControlEditor editor;

    public CheckoutAction() {
        setImageDescriptor(imageHelper.getImageDescriptor("images/vc/checkout.gif")); //$NON-NLS-1$
        setToolTipText(Messages.getString("CheckoutAction.ToolTipText")); //$NON-NLS-1$
    }

    public CheckoutAction(final FindInSourceControlEditor editor) {
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
        final TypedServerItem[] selectedItems = editor.getSelectedServerItems();
        return (selectedItems != null && selectedItems.length > 0 && !hasConflictingChange());
    }

    @Override
    public void run() {
        if (editor == null || editor.getEditorInput() == null) {
            return;
        }

        final FindInSourceControlEditorInput editorInput = editor.getEditorInput();
        final TypedServerItem[] serverItems = editor.getSelectedServerItems();

        if (serverItems != null && serverItems.length > 0) {
            final TypedItemSpec[] itemSpecs = getItemSpecs(editorInput, serverItems);

            new CheckoutWithPromptTask(editor.getSite().getShell(), editorInput.getRepository(), itemSpecs).run();
        }

        editor.run();
    }

    /**
     * returns the list of item specs, if an item has a rename pending change in
     * the current workspace it uses the new name
     *
     */
    private TypedItemSpec[] getItemSpecs(
        final FindInSourceControlEditorInput editorInput,
        final TypedServerItem[] serverItems) {

        final ArrayList<TypedItemSpec> itemSpecs = new ArrayList<TypedItemSpec>();

        editorInput.getRepository().getPendingChangeCache();
        for (final TypedServerItem serverItem : serverItems) {
            final String itemPath = serverItem.getServerPath();

            final TypedItemSpec itemSpec = new TypedItemSpec(
                itemPath,
                serverItem.getType() == ServerItemType.FILE ? RecursionType.NONE : RecursionType.FULL,
                serverItem.getType() == ServerItemType.FILE ? ItemType.FILE : ItemType.FOLDER);
            itemSpecs.add(itemSpec);
        }

        return itemSpecs.toArray(new TypedItemSpec[itemSpecs.size()]);
    }

    /*
     * Checks if the selected item is currently checked out or has pending
     * delete by the current user in the current workspace
     */
    private boolean hasConflictingChange() {
        final Map<PendingChange, PendingSet> selectedPendingChanges = editor.getSelectedPendingChanges();
        final Workspace currentWorkspace = editor.getEditorInput().getRepository().getWorkspace();

        if (selectedPendingChanges != null && !selectedPendingChanges.isEmpty()) {
            for (final PendingChange pendingChange : selectedPendingChanges.keySet()) {
                final PendingSet pendingSet = selectedPendingChanges.get(pendingChange);
                if ((pendingChange.getChangeType().contains(ChangeType.EDIT)
                    || pendingChange.getChangeType().contains(ChangeType.DELETE))
                    && pendingSet.getName().equals(currentWorkspace.getName())
                    && pendingSet.getOwnerName().equals(currentWorkspace.getOwnerName())) {
                    return true;
                }
            }
        }

        return false;
    }
}
