// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.repository;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.repository.PendingChangeRepositoryMap;
import com.microsoft.tfs.client.common.repository.RepositoryMap;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class ResourceRepositoryMap extends RepositoryMap {
    public ResourceRepositoryMap() {
        super(IResource.class);
    }

    public TFSRepository getRepository(final IResource resource) {
        return getRepositoryInternal(resource);
    }

    public IResource[] getResources(final TFSRepository repository) {
        return (IResource[]) getMappedObjectsInternal(repository);
    }

    public IResource[] getAllResources() {
        return (IResource[]) getAllMappedObjectsInternal();
    }

    public ResourceRepositoryMap subMap(final IResource[] subset) {
        return (ResourceRepositoryMap) subMapInternal(subset);
    }

    public void addMappings(final TFSRepository repository, final IResource[] resources) {
        addMappingsInternal(repository, resources);
    }

    public PendingChangeRepositoryMap getPendingChangesMap() {
        final PendingChangeRepositoryMap resultMap = new PendingChangeRepositoryMap();

        final TFSRepository[] repositories = getRepositories();
        for (int i = 0; i < repositories.length; i++) {
            final IResource[] resources = getResources(repositories[i]);
            for (int j = 0; j < resources.length; j++) {
                final PendingChange[] changesForResource =
                    PluginResourceHelpers.pendingChangesForResource(resources[j], repositories[i]);

                resultMap.addMappings(repositories[i], changesForResource);
            }
        }

        return resultMap;
    }
}
