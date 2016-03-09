// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

import java.util.EventObject;

public class RepositoryManagerEvent extends EventObject {
    private final TFSRepository repository;

    public RepositoryManagerEvent(final RepositoryManager source, final TFSRepository repository) {
        super(source);

        this.repository = repository;
    }

    public RepositoryManager getRepositoryManager() {
        return (RepositoryManager) getSource();
    }

    public TFSRepository getRepository() {
        return repository;
    }
}
