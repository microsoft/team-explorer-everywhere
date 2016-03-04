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

package com.microsoft.teamfoundation.sourcecontrol.webapi.model;

import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;

/** 
 */
public class VersionControlProjectInfo {

    private TeamProjectReference project;
    private boolean supportsGit;
    private boolean supportsTFVC;

    public TeamProjectReference getProject() {
        return project;
    }

    public void setProject(final TeamProjectReference project) {
        this.project = project;
    }

    public boolean getSupportsGit() {
        return supportsGit;
    }

    public void setSupportsGit(final boolean supportsGit) {
        this.supportsGit = supportsGit;
    }

    public boolean getSupportsTFVC() {
        return supportsTFVC;
    }

    public void setSupportsTFVC(final boolean supportsTFVC) {
        this.supportsTFVC = supportsTFVC;
    }
}
