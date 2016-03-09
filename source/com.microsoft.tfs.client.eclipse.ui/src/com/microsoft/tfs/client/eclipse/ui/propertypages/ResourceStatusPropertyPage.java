// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.eclipse.repository.ResourceRepositoryMap;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

public class ResourceStatusPropertyPage extends PropertyPage {
    public ResourceStatusPropertyPage() {
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        SWTUtil.gridLayout(composite);

        final IResource resource = getResource();

        final ResourceRepositoryMap resourceRepositoryMap = PluginResourceHelpers.mapResources(new IResource[] {
            resource
        });

        StatusItem[] statusItems = new StatusItem[0];

        final TFSRepository repository = resourceRepositoryMap.getRepository(resource);
        if (repository != null) {
            statusItems = getStatusItems(resource, repository);
        }

        if (statusItems.length == 0) {
            SWTUtil.createLabel(composite, Messages.getString("ResourceStatusPropertyPage.NoPendingChangesLabelText")); //$NON-NLS-1$
        } else {
            SWTUtil.createLabel(composite, Messages.getString("ResourceStatusPropertyPage.StatusLabelText")); //$NON-NLS-1$

            final StatusItemTable table = new StatusItemTable(composite, SWT.NONE);
            GridDataBuilder.newInstance().grab().fill().applyTo(table);

            table.setStatusItems(statusItems);
        }

        return composite;
    }

    private StatusItem[] getStatusItems(final IResource resource, final TFSRepository repository) {
        final List statusItems = new ArrayList();

        final String authenticatedUserName =
            repository.getWorkspace().getClient().getConnection().getAuthorizedIdentity().getDisplayName();
        final String location = Resources.getLocation(resource, LocationUnavailablePolicy.THROW);

        final PendingChange localPendingChange =
            repository.getPendingChangeCache().getPendingChangeByLocalPath(location);

        if (localPendingChange != null) {
            final StatusItem statusItem = new StatusItem(
                authenticatedUserName,
                localPendingChange.getChangeType(),
                repository.getWorkspace().getName(),
                localPendingChange.getPropertyValues());
            statusItems.add(statusItem);
        }

        final PendingSet[] pendingSets = repository.getWorkspace().queryPendingSets(new String[] {
            location
        }, RecursionType.NONE, null, null, false);

        if (pendingSets != null) {
            for (int i = 0; i < pendingSets.length; i++) {
                final PendingSet pendingSet = pendingSets[i];
                final PendingChange[] pendingChanges = pendingSet.getPendingChanges();
                for (int j = 0; j < pendingChanges.length; j++) {
                    if (localPendingChange == null
                        || localPendingChange.getPendingChangeID() != pendingChanges[j].getPendingChangeID()) {
                        final StatusItem statusItem = new StatusItem(
                            pendingSet.getOwnerDisplayName(),
                            pendingChanges[j].getChangeType(),
                            pendingSet.getName(),
                            pendingChanges[j].getPropertyValues());
                        statusItems.add(statusItem);
                    }
                }
            }
        }

        return (StatusItem[]) statusItems.toArray(new StatusItem[statusItems.size()]);
    }

    private IResource getResource() {
        return (IResource) getElement().getAdapter(IResource.class);
    }
}
