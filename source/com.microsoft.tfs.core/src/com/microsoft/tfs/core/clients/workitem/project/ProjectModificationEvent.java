// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.project;

/**
 * @since TEE-SDK-10.1
 */
public class ProjectModificationEvent {
    private Project project;

    public ProjectModificationEvent(final Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }
}
