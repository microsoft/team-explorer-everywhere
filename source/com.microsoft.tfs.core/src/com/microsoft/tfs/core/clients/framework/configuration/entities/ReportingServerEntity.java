// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.entities;

import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;

/**
 * @since TEE-SDK-10.1
 */
public interface ReportingServerEntity extends TFSEntity {
    public String getDefaultItemPath();

    public String getReportManagerURL();

    public ServiceDefinition getReportManagerDefinition();

    public String getReportWebServiceURL();

    public ServiceDefinition getReportWebServiceDefinition();
}
