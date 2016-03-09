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

package com.microsoft.teamfoundation.core.webapi.types.model;

import java.util.UUID;

/** 
 * The Team Context for an operation.
 * 
 */
public class TeamContext {

    /**
    * The team project Id or name.  Ignored if ProjectId is set.
    */
    private String project;
    /**
    * The Team Project ID.  Required if Project is not set.
    */
    private UUID projectId;
    /**
    * The Team Id or name.  Ignored if TeamId is set.
    */
    private String team;
    /**
    * The Team Id
    */
    private UUID teamId;

    /**
    * The team project Id or name.  Ignored if ProjectId is set.
    */
    public String getProject() {
        return project;
    }

    /**
    * The team project Id or name.  Ignored if ProjectId is set.
    */
    public void setProject(final String project) {
        this.project = project;
    }

    /**
    * The Team Project ID.  Required if Project is not set.
    */
    public UUID getProjectId() {
        return projectId;
    }

    /**
    * The Team Project ID.  Required if Project is not set.
    */
    public void setProjectId(final UUID projectId) {
        this.projectId = projectId;
    }

    /**
    * The Team Id or name.  Ignored if TeamId is set.
    */
    public String getTeam() {
        return team;
    }

    /**
    * The Team Id or name.  Ignored if TeamId is set.
    */
    public void setTeam(final String team) {
        this.team = team;
    }

    /**
    * The Team Id
    */
    public UUID getTeamId() {
        return teamId;
    }

    /**
    * The Team Id
    */
    public void setTeamId(final UUID teamId) {
        this.teamId = teamId;
    }
}
