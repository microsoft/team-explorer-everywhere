// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.entities;

import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public interface ProjectPortalEntity extends TFSEntity {
    public String getResourceSubType();

    public GUID getOwnedWebIdentifier();

    public String getRelativePath();

    public TFSEntity getReferencedResource();

    public String getFullItemPath();
}
