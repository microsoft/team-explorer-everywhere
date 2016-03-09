// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pguidance;

import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;

/**
 * Client for the TFS process guidance resources.
 *
 * @since TEE-SDK-10.1
 */
public interface IProcessGuidance {
    public boolean isEnabled(ProjectInfo projectInfo);

    public ProcessGuidanceURLInfo getProcessGuidanceURL(ProjectInfo projectInfo, String documentPath);

    public ProcessGuidanceURLInfo getProcessGuidanceURL(
        ProjectInfo projectInfo,
        String documentPath,
        String[] alternateDocumentPaths);
}
