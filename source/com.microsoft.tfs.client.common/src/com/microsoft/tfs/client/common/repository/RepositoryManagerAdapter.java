// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

public abstract class RepositoryManagerAdapter implements RepositoryManagerListener {
    @Override
    public void onRepositoryAdded(final RepositoryManagerEvent event) {
    }

    @Override
    public void onRepositoryRemoved(final RepositoryManagerEvent event) {
    }

    @Override
    public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
    }
}
