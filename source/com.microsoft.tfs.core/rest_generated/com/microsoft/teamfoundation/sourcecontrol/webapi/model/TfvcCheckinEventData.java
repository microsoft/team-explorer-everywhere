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
public class TfvcCheckinEventData {

    private TfvcChangeset changeset;
    private TeamProjectReference project;

    public TfvcChangeset getChangeset() {
        return changeset;
    }

    public void setChangeset(final TfvcChangeset changeset) {
        this.changeset = changeset;
    }

    public TeamProjectReference getProject() {
        return project;
    }

    public void setProject(final TeamProjectReference project) {
        this.project = project;
    }
}
