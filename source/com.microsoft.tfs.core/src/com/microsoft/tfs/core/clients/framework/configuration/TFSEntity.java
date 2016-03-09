// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration;

import com.microsoft.tfs.util.GUID;

/**
 * A hierarchical node in the configuration tree.
 *
 * @since TEE-SDK-10.1
 */
public interface TFSEntity {
    public GUID getResourceID();

    public String getDisplayName();

    public String getDisplayPath();

    public String getDescription();

    public TFSEntity getParent();
}
