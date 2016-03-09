// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.commands.vc.GetPendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemType;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;

public class ChangeItemGeneralPropertyPage extends BaseGeneralPropertyPage {
    @Override
    protected void doCreateContents(final Composite parent) {
        final ChangeItem changeItem = getChangeItem();

        // Can't query an Item for pending adds
        final Item item =
            (changeItem.getType() == ChangeItemType.PENDING && changeItem.getChangeType().contains(ChangeType.ADD))
                ? null : getLatestItem(changeItem);

        addRow(
            parent,
            GeneralPropertyRowID.NAME_SERVER,
            Messages.getString("ChangeItemGeneralPropertyPage.ServerNameLabelText"), //$NON-NLS-1$
            changeItem.getServerItem());

        if (ChangeItemType.PENDING == changeItem.getType()) {
            addRow(
                parent,
                GeneralPropertyRowID.NAME_LOCAL,
                Messages.getString("ChangeItemGeneralPropertyPage.LocalNameLabelText"), //$NON-NLS-1$
                changeItem.getPendingChange().getLocalItem());
        }

        if (item != null) {
            addRow(
                parent,
                GeneralPropertyRowID.VERSION_LATEST,
                Messages.getString("ChangeItemGeneralPropertyPage.LatestVersionLabelText"), //$NON-NLS-1$
                String.valueOf(item.getChangeSetID()));
        }

        if (ChangeItemType.PENDING == changeItem.getType()) {
            addRow(
                parent,
                GeneralPropertyRowID.VERSION_WORKSPACE,
                Messages.getString("ChangeItemGeneralPropertyPage.WorkspaceLabelText"), //$NON-NLS-1$
                String.valueOf(changeItem.getVersion()));
        } else if (ChangeItemType.SHELVESET == changeItem.getType()) {
            addRow(
                parent,
                GeneralPropertyRowID.VERSION_SHELVESET,
                Messages.getString("ChangeItemGeneralPropertyPage.ShelvesetVersionLabelText"), //$NON-NLS-1$
                String.valueOf(changeItem.getVersion()));
        } else if (ChangeItemType.CHANGESET == changeItem.getType()) {
            addRow(
                parent,
                GeneralPropertyRowID.VERSION_CHANGESET,
                Messages.getString("ChangeItemGeneralPropertyPage.ChangesetVersionLabelText"), //$NON-NLS-1$
                String.valueOf(changeItem.getVersion()));
        }

        if (changeItem.getItemType() == ItemType.FILE) {
            int encoding = changeItem.getEncoding();

            /*
             * Note: ChangeItem has an unreliable encoding. Since it may be
             * constructed using core's fake pending change events (instead of
             * PendingChange objects from the server), it may simply be -2
             * (unchanged) for pended edits and adds. It will only necessarily
             * have an encoding when we have an encoding change pended. Thus, we
             * must query the item to get its encoding in most cases.
             */
            if (changeItem.getChangeType().contains(ChangeType.ADD)
                || !changeItem.getChangeType().contains(ChangeType.ENCODING)) {
                final ExtendedItem[] extendedItem = changeItem.getRepository().getWorkspace().getExtendedItems(
                    changeItem.getLocalOrServerItem(),
                    DeletedState.NON_DELETED,
                    ItemType.FILE);

                if (extendedItem != null && extendedItem.length == 1) {
                    encoding = extendedItem[0].getEncoding().getCodePage();
                }
            }

            addRow(
                parent,
                GeneralPropertyRowID.ENCODING,
                Messages.getString("ChangeItemGeneralPropertyPage.EncodingLabelText"), //$NON-NLS-1$
                encodingToString(encoding));

            // Call after add row
            final PropertyValue[] itemProperties = ChangeItemType.PENDING == changeItem.getType()
                ? changeItem.getPropertyValues() : getItemProperties(changeItem);
            setItem(
                changeItem.getRepository(),
                changeItem.getLocalOrServerItem(),
                new FileEncoding(encoding),
                containsExecutableProperty(itemProperties));
        } else {
            addRow(
                parent,
                GeneralPropertyRowID.ENCODING,
                Messages.getString("ChangeItemGeneralPropertyPage.EncodingLabelText"), //$NON-NLS-1$
                Messages.getString("ChangeItemGeneralPropertyPage.EncodingNotApplicableLabelText")); //$NON-NLS-1$
        }
    }

    private Item getLatestItem(final ChangeItem changeItem) {
        final VersionControlClient vcClient = changeItem.getRepository().getVersionControlClient();
        return vcClient.getItem(changeItem.getItemID(), Integer.MAX_VALUE, false);
    }

    private PropertyValue[] getItemProperties(final ChangeItem item) {
        // Only query for properties if the server supports them
        String[] itemPropertyFilters = null;
        if (item.getRepository().getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
            itemPropertyFilters = new String[] {
                PropertyConstants.EXECUTABLE_KEY
            };
        }

        // in case the item does not have pending changes
        final QueryItemsExtendedCommand queryCommand =
            new QueryItemsExtendedCommand(item.getRepository(), new ItemSpec[] {
                new ItemSpec(item.getServerItem(), RecursionType.NONE)
        }, DeletedState.NON_DELETED, ItemType.FILE, GetItemsOptions.NONE, itemPropertyFilters);

        final IStatus status = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(queryCommand);

        if (status.isOK()) {
            final ExtendedItem[][] results = queryCommand.getItems();

            if (results != null && results.length > 0 && results[0] != null && results[0].length > 0) {
                return results[0][0].getPropertyValues();

            }
        }
        return null;
    }

    private ChangeItem getChangeItem() {
        final ChangeItem item = (ChangeItem) getElement().getAdapter(ChangeItem.class);

        // Only query for properties if the server supports them
        String[] itemPropertyFilters = null;
        if (item.getRepository().getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
            itemPropertyFilters = new String[] {
                PropertyConstants.EXECUTABLE_KEY
            };
        }

        // Refresh the item to get properties needed for this page
        final GetPendingChangesCommand command = new GetPendingChangesCommand(item.getRepository(), new ItemSpec[] {
            new ItemSpec(item.getServerItem(), RecursionType.NONE)
        }, false, itemPropertyFilters);

        final IStatus status = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(command);

        if (status.isOK()) {
            final PendingSet set = command.getPendingSet();
            if (set != null && set.getPendingChanges() != null && set.getPendingChanges().length > 0) {
                return new ChangeItem(set.getPendingChanges()[0], ChangeItemType.PENDING, item.getRepository());
            }
        }

        // Query failed for some reason (change disappeared?); use the old one
        return item;
    }
}
