// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.commands.vc.GetPendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.client.eclipse.repository.ResourceRepositoryMap;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;

public class ResourceGeneralPropertyPage extends BaseGeneralPropertyPage {
    @Override
    protected void doCreateContents(final Composite parent) {
        final IResource resource = getResource();

        final ResourceRepositoryMap resourceRepositoryMap = PluginResourceHelpers.mapResources(new IResource[] {
            resource
        });

        final TFSRepository repository = resourceRepositoryMap.getRepository(resource);

        if (repository == null) {
            return;
        }

        final String itemPath = Resources.getLocation(resource, LocationUnavailablePolicy.THROW);

        final TypedItemSpec typedItemSpec =
            PluginResourceHelpers.typedItemSpecForResource(resource, false, LocationUnavailablePolicy.IGNORE_RESOURCE);

        if (typedItemSpec == null) {
            return;
        }

        // Only query for properties if the server supports them
        String[] itemPropertyFilters = null;
        if (repository.getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
            itemPropertyFilters = new String[] {
                PropertyConstants.EXECUTABLE_KEY
            };
        }

        final QueryItemsExtendedCommand command = new QueryItemsExtendedCommand(repository, new ItemSpec[] {
            new ItemSpec(itemPath, RecursionType.NONE)
        }, DeletedState.NON_DELETED, typedItemSpec.getType(), GetItemsOptions.NONE, itemPropertyFilters);

        final IStatus status = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(command);

        ExtendedItem extendedItem = null;
        if (status.isOK()) {
            final ExtendedItem[][] results = command.getItems();

            if (results != null && results.length > 0 && results[0] != null && results[0].length > 0) {
                extendedItem = results[0][0];
            }
        }

        // Query failed
        if (extendedItem == null) {
            createUnmanagedLabel(parent);
            return;
        }

        String serverItem = extendedItem.getSourceServerItem();
        if (serverItem == null) {
            serverItem = extendedItem.getTargetServerItem();
        }

        addRow(
            parent,
            GeneralPropertyRowID.NAME_SERVER,
            Messages.getString("ResourceGeneralPropertyPage.ServerNameLabelText"), //$NON-NLS-1$
            serverItem);
        addRow(
            parent,
            GeneralPropertyRowID.NAME_LOCAL,
            Messages.getString("ResourceGeneralPropertyPage.LocalNameLabelText"), //$NON-NLS-1$
            extendedItem.getLocalItem());
        addRow(
            parent,
            GeneralPropertyRowID.VERSION_LATEST,
            Messages.getString("ResourceGeneralPropertyPage.LatestVersionLabelText"), //$NON-NLS-1$
            String.valueOf(extendedItem.getLatestVersion()));
        addRow(
            parent,
            GeneralPropertyRowID.VERSION_LOCAL,
            Messages.getString("ResourceGeneralPropertyPage.LocalVersionLabelText"), //$NON-NLS-1$
            String.valueOf(extendedItem.getLocalVersion()));

        if (extendedItem.getItemType() == ItemType.FILE) {
            /*
             * There's a TFS server bug that an extended item's encoding doesn't
             * include the encoding set in any pending encoding changes on it.
             *
             * Query the pending change from the server to get the actual value.
             * The local pending change cache can't be used because it also has
             * encoding freshness issues.
             */
            PendingChange pendingChange = null;

            final GetPendingChangesCommand pendingChangeCommand =
                new GetPendingChangesCommand(repository, new ItemSpec[] {
                    new ItemSpec(serverItem, RecursionType.NONE)
            }, false, null);

            final IStatus pendingChangeStatus =
                UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(pendingChangeCommand);

            if (pendingChangeStatus.isOK()) {
                final PendingSet set = pendingChangeCommand.getPendingSet();
                if (set != null && set.getPendingChanges() != null && set.getPendingChanges().length > 0) {
                    pendingChange = set.getPendingChanges()[0];
                }
            }

            if (pendingChange != null) {
                addRow(
                    parent,
                    GeneralPropertyRowID.ENCODING,
                    Messages.getString("ResourceGeneralPropertyPage.EncodingLabelText"), //$NON-NLS-1$
                    encodingToString(pendingChange.getEncoding()));

                // Call after add encoding row
                setItem(
                    repository,
                    extendedItem.getLocalItem(),
                    new FileEncoding(pendingChange.getEncoding()),
                    containsExecutableProperty(extendedItem.getPropertyValues()));
            } else {
                addRow(
                    parent,
                    GeneralPropertyRowID.ENCODING,
                    Messages.getString("ResourceGeneralPropertyPage.EncodingLabelText"), //$NON-NLS-1$
                    encodingToString(extendedItem.getEncoding().getCodePage()));

                // Call after add encoding row
                setItem(
                    repository,
                    extendedItem.getLocalItem(),
                    extendedItem.getEncoding(),
                    containsExecutableProperty(extendedItem.getPropertyValues()));
            }
        } else {
            addRow(
                parent,
                GeneralPropertyRowID.ENCODING,
                Messages.getString("ResourceGeneralPropertyPage.EncodingLabelText"), //$NON-NLS-1$
                Messages.getString("ResourceGeneralPropertyPage.EncodingNotApplicableText")); //$NON-NLS-1$
        }
    }

    private void createUnmanagedLabel(final Composite parent) {
        final Label label =
            SWTUtil.createLabel(parent, Messages.getString("ResourceGeneralPropertyPage.UnmanagedLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(label);
    }

    private IResource getResource() {
        return (IResource) getElement().getAdapter(IResource.class);
    }
}
