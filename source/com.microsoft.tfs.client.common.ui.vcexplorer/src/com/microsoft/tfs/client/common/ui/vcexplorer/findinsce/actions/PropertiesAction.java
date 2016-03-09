// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.GeneralBranchPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.GeneralPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.RelationshipPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.StatusPropertiesTab;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.branches.BranchesPropertiesTab;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PropertiesDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemFactory;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditorInput;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

public class PropertiesAction extends Action {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private FindInSourceControlEditor editor;

    public PropertiesAction() {
        setImageDescriptor(imageHelper.getImageDescriptor("images/vc/properties.gif")); //$NON-NLS-1$
        setToolTipText(Messages.getString("PropertiesAction.ToolTipText")); //$NON-NLS-1$
    }

    public PropertiesAction(final FindInSourceControlEditor editor) {
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

        if (editorInput == null || serverItems == null || serverItems.length == 0) {
            return;
        }

        // retrieve the item server path
        final String itemServerPath = serverItems[0].getServerPath();

        final QueryItemsExtendedCommand queryCommand = new QueryItemsExtendedCommand(
            editorInput.getRepository(),
            itemServerPath,
            serverItems[0].getType() == ServerItemType.FILE ? ItemType.FILE : ItemType.FOLDER,
            DeletedState.NON_DELETED,
            RecursionType.NONE,
            GetItemsOptions.NONE);

        final IStatus queryStatus =
            UICommandExecutorFactory.newUICommandExecutor(editor.getSite().getShell()).execute(queryCommand);

        if (!queryStatus.isOK()) {
            return;
        }

        if (queryCommand.getItems().length != 1 || queryCommand.getItems()[0].length != 1) {
            MessageDialog.openError(
                editor.getSite().getShell(),
                Messages.getString("PropertiesAction.QueryErrorTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("PropertiesAction.QueryErrorMessageFormat"), //$NON-NLS-1$
                    serverItems[0].getServerPath()));
            return;
        }

        final TFSItem item = TFSItemFactory.getItem(queryCommand.getItems()[0][0], editorInput.getRepository());
        final ItemIdentifier itemId = null;

        if (item != null || itemId != null) {
            final PropertiesDialog propertiesDialog =
                new PropertiesDialog(editor.getSite().getShell(), editorInput.getRepository(), item, itemId);

            if (item.getExtendedItem() != null && item.getExtendedItem().isBranch()) {
                propertiesDialog.addPropertiesTab(new GeneralBranchPropertiesTab());
                propertiesDialog.addPropertiesTab(new RelationshipPropertiesTab());
            } else {
                propertiesDialog.addPropertiesTab(new GeneralPropertiesTab());
                propertiesDialog.addPropertiesTab(new StatusPropertiesTab());
                propertiesDialog.addPropertiesTab(new BranchesPropertiesTab());
            }

            propertiesDialog.open();
        }
    }
}
