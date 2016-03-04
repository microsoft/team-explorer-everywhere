// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.core.webapi.model;


/** 
 */
public class ProjectMessage {

    private ProjectInfo project;
    private ProjectChangeType projectChangeType;

    public ProjectInfo getProject() {
        return project;
    }

    public void setProject(final ProjectInfo project) {
        this.project = project;
    }

    public ProjectChangeType getProjectChangeType() {
        return projectChangeType;
    }

    public void setProjectChangeType(final ProjectChangeType projectChangeType) {
        this.projectChangeType = projectChangeType;
    }
}
