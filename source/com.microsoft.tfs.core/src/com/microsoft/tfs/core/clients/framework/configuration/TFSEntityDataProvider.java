// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration;

import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public interface TFSEntityDataProvider {
    public GUID getResourceTypeID();

    public String getDisplayName();

    public String getDescription();

    public String getProperty(String propertyName);

    public ServiceDefinition getServiceReference(String serviceName);
}
