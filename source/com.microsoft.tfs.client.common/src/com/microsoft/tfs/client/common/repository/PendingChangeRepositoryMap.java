// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class PendingChangeRepositoryMap extends RepositoryMap {
    public PendingChangeRepositoryMap() {
        super(PendingChange.class);
    }

    public TFSRepository getRepository(final PendingChange change) {
        return getRepositoryInternal(change);
    }

    public PendingChange[] getChanges(final TFSRepository repository) {
        return (PendingChange[]) getMappedObjectsInternal(repository);
    }

    public PendingChange[] getAllChanges() {
        return (PendingChange[]) getAllMappedObjectsInternal();
    }

    public PendingChangeRepositoryMap subMap(final PendingChange[] subset) {
        return (PendingChangeRepositoryMap) subMapInternal(subset);
    }

    public void addMappings(final TFSRepository repository, final PendingChange[] changes) {
        addMappingsInternal(repository, changes);
    }
}
