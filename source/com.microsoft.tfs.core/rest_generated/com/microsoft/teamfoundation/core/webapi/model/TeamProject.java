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

import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 * Represents a Team Project object.
 * 
 */
public class TeamProject
    extends TeamProjectReference {

    /**
    * The links to other objects related to this object.
    */
    private ReferenceLinks _links;
    private HashMap<String,HashMap<String,String>> capabilities;
    /**
    * The shallow ref to the default team.
    */
    private WebApiTeamRef defaultTeam;

    /**
    * The links to other objects related to this object.
    */
    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    /**
    * The links to other objects related to this object.
    */
    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    public HashMap<String,HashMap<String,String>> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(final HashMap<String,HashMap<String,String>> capabilities) {
        this.capabilities = capabilities;
    }

    /**
    * The shallow ref to the default team.
    */
    public WebApiTeamRef getDefaultTeam() {
        return defaultTeam;
    }

    /**
    * The shallow ref to the default team.
    */
    public void setDefaultTeam(final WebApiTeamRef defaultTeam) {
        this.defaultTeam = defaultTeam;
    }
}
