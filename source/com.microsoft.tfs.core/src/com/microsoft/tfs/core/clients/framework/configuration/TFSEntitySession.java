// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration;

import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;

/**
 * @since TEE-SDK-10.1
 */
public interface TFSEntitySession {
    public OrganizationalRootEntity getOrganizationalRoot();
}