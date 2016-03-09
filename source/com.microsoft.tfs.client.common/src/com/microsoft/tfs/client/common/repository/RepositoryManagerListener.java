// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

import java.util.EventListener;

public interface RepositoryManagerListener extends EventListener {
    public void onRepositoryAdded(RepositoryManagerEvent event);

    public void onRepositoryRemoved(RepositoryManagerEvent event);

    public void onDefaultRepositoryChanged(RepositoryManagerEvent event);
}
