// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.entities;

import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-10.1
 */
public interface TeamProjectEntity extends TFSEntity {
    public GUID getProjectID();

    public String getProjectName();

    public String getProjectURI();

    public ProcessGuidanceEntity getProcessGuidance();

    public ProjectPortalEntity getProjectPortal();

    public ReportingFolderEntity getReportingFolder();
}
