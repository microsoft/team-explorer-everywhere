// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.entities;

import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public interface TeamFoundationServerEntity extends TFSEntity {
    public ProjectCollectionEntity[] getProjectCollections();

    public ProjectCollectionEntity getProjectCollection(GUID instanceId);
}
