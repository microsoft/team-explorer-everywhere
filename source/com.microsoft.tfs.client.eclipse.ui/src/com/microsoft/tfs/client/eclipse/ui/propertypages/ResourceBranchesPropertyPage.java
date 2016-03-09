// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IWorkbenchPropertyPage;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.branches.BranchesPropertiesTab;
import com.microsoft.tfs.client.eclipse.repository.ResourceRepositoryMap;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;

public class ResourceBranchesPropertyPage extends BaseBranchesPropertyPage implements IWorkbenchPropertyPage {
    private final static Log log = LogFactory.getLog(BranchesPropertiesTab.class);

    private final boolean hasComputed = false;
    private TFSRepository repository;
    private String location;

    @Override
    protected TFSRepository getRepository() {
        compute();

        return repository;
    }

    @Override
    protected String getLocation() {
        compute();

        return location;
    }

    private void compute() {
        if (hasComputed) {
            return;
        }

        final IResource resource = getResource();
        final ResourceRepositoryMap resourceRepositoryMap = PluginResourceHelpers.mapResources(new IResource[] {
            resource
        });

        repository = resourceRepositoryMap.getRepository(resource);
        location = Resources.getLocation(resource, LocationUnavailablePolicy.IGNORE_RESOURCE);
    }

    private IResource getResource() {
        return (IResource) getElement().getAdapter(IResource.class);
    }
}
