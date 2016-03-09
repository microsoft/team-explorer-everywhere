// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

public interface RepositoryManagerProvider {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.RepositoryManagerProvider"; //$NON-NLS-1$

    public RepositoryManager getRepositoryManager();
}
